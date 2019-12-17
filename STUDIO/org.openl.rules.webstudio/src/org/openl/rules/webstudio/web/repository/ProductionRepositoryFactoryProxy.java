package org.openl.rules.webstudio.web.repository;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.util.IOUtils;
import org.springframework.core.env.PropertyResolver;

/**
 * Repository Factory Proxy.
 * <p/>
 * Takes actual factory description from <i>rules-production.properties</i> file.
 */
public class ProductionRepositoryFactoryProxy {

    private Map<String, Repository> factories = new HashMap<>();

    private PropertyResolver propertyResolver;

    public ProductionRepositoryFactoryProxy(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    public Repository getRepositoryInstance(String configName) throws RRepositoryException {
        if (!factories.containsKey(configName)) {
            synchronized (this) {
                if (!factories.containsKey(configName)) {
                    factories.put(configName, createFactory(configName));
                }
            }
        }

        return factories.get(configName);
    }

    public void releaseRepository(String configName) {
        synchronized (this) {
            Repository repository = factories.get(configName);
            if (repository != null) {
                if (repository instanceof Closeable) {
                    // Close repo connection after validation
                    IOUtils.closeQuietly((Closeable) repository);
                }
                factories.remove(configName);
            }
        }
    }

    public void destroy() {
        synchronized (this) {
            for (Repository repository : factories.values()) {
                if (repository instanceof Closeable) {
                    // Close repo connection after validation
                    IOUtils.closeQuietly((Closeable) repository);
                }
            }
            factories.clear();
        }
    }

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    private Repository createFactory(String configName) throws RRepositoryException {
        return RepositoryInstatiator.newRepository(configName, propertyResolver);
    }

    public boolean isIncludeVersionInDeploymentName(String configName) {
        return Boolean.parseBoolean(propertyResolver.getProperty(RepositorySettings.VERSION_IN_DEPLOYMENT_NAME));
    }

    public String getDeploymentsPath(String configName) {
        String deployPath = propertyResolver.getProperty("repository." + configName + ".base.path");
        return deployPath.isEmpty() || deployPath.endsWith("/") ? deployPath : deployPath + "/";
    }
}
