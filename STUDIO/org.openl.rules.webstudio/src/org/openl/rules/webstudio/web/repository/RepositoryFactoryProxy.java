package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.DESIGN_REPOSITORY_CONFIGS;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.DESIGN_REPOSITORY_CONFIGS;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.util.IOUtils;
import org.springframework.core.env.PropertyResolver;

/**
 * Repository Factory Proxy.
 * <p/>
 * Takes actual factory description from the environment in which the current application is running.
 */
public class RepositoryFactoryProxy {

    private final Map<String, Repository> factories = new HashMap<>();

    private final PropertyResolver propertyResolver;

    private final String repoListConfig;

    public RepositoryFactoryProxy(PropertyResolver propertyResolver, RepositoryMode mode) {
        switch (mode) {
            case DESIGN:
                repoListConfig = DESIGN_REPOSITORY_CONFIGS;
                break;
            case PRODUCTION:
                repoListConfig = PRODUCTION_REPOSITORY_CONFIGS;
                break;
            default:
                throw new IllegalArgumentException("Repository mode " + mode + " isn't supported");
        }
        this.propertyResolver = RepositoryEditor.createPropertiesWrapper(propertyResolver, repoListConfig);
    }

    public String getRepoListConfig() {
        return repoListConfig;
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

    private Repository createFactory(String configName) throws RRepositoryException {
        return RepositoryInstatiator.newRepository(configName, propertyResolver);
    }

    public boolean isIncludeVersionInDeploymentName(String configName) {
        return Boolean
            .parseBoolean(propertyResolver.getProperty("repository." + configName + ".version-in-deployment-name"));
    }

    public String getBasePath(String configName) {
        String key = "repository." + configName + ".base.path";
        String basePath = propertyResolver.getProperty(key);
        if (basePath == null) {
            throw new IllegalArgumentException("Property " + key + " is absent");
        }
        return basePath.isEmpty() || basePath.endsWith("/") ? basePath : basePath + "/";
    }
}
