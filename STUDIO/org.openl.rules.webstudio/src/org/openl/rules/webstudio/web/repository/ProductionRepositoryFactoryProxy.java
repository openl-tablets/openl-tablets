package org.openl.rules.webstudio.web.repository;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.admin.ProductionRepositoryEditor;
import org.openl.util.IOUtils;
import org.springframework.core.env.PropertyResolver;

/**
 * Repository Factory Proxy.
 * <p/>
 * Takes actual factory description from the environment in which the current application is running.
 */
public class ProductionRepositoryFactoryProxy {

    private Map<String, Repository> factories = new HashMap<>();

    private final PropertyResolver propertyResolver;

    public ProductionRepositoryFactoryProxy(PropertyResolver propertyResolver) {
        this.propertyResolver = ProductionRepositoryEditor.createProductionPropertiesWrapper(propertyResolver);
    }

    public Repository getRepositoryInstance(String configName) {
        if (!factories.containsKey(configName)) {
            synchronized (this) {
                if (!factories.containsKey(configName)) {
                    factories.put(configName, RepositoryInstatiator.newRepository(RepositoryInstatiator.REPOSITORY_PREFIX + configName, propertyResolver::getProperty));
                }
            }
        }

        return factories.get(configName);
    }

    public void releaseRepository(String configName) {
        synchronized (this) {
            Repository repository = factories.get(configName);
            if (repository != null) {
                // Close repo connection after validation
                IOUtils.closeQuietly(repository);
                factories.remove(configName);
            }
        }
    }

    public void destroy() {
        synchronized (this) {
            for (Repository repository : factories.values()) {
                // Close repo connection after validation
                IOUtils.closeQuietly(repository);
            }
            factories.clear();
        }
    }

    public String getDeploymentsPath(String configName) {
        String key = "repository." + configName + ".base.path";
        String deployPath = propertyResolver.getProperty(key);
        if (deployPath == null) {
            throw new IllegalArgumentException("Property " + key + " is absent");
        }
        return deployPath.isEmpty() || deployPath.endsWith("/") ? deployPath : deployPath + "/";
    }
}
