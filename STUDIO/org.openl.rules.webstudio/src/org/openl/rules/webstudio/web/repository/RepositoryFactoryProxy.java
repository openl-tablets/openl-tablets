package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.DESIGN_REPOSITORY_CONFIGS;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.Repository;
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

    private final String defaultBasePathConfig;

    public RepositoryFactoryProxy(PropertyResolver propertyResolver, RepositoryMode mode) {
        switch (mode) {
            case DESIGN:
                repoListConfig = DESIGN_REPOSITORY_CONFIGS;
                defaultBasePathConfig = "repo-default.design.base.path";
                break;
            case PRODUCTION:
                repoListConfig = PRODUCTION_REPOSITORY_CONFIGS;
                defaultBasePathConfig = "repo-default.production.base.path";
                break;
            default:
                throw new IllegalArgumentException("Repository mode " + mode + " is not supported");
        }
        this.propertyResolver = propertyResolver;
    }

    public String getRepoListConfig() {
        return repoListConfig;
     }

    public Repository getRepositoryInstance(String configName) {
        if (!factories.containsKey(Objects.requireNonNull(configName))) {
            synchronized (this) {
                if (!factories.containsKey(configName)) {
                    factories.put(configName, RepositoryInstatiator.newRepository(Comments.REPOSITORY_PREFIX + configName, propertyResolver::getProperty));
                }
            }
        }

        return factories.get(configName);
    }

    public void releaseRepository(String configName) {
        synchronized (this) {
            Repository repository = factories.remove(configName);
            if (repository != null) {
                // Close repo connection after validation
                IOUtils.closeQuietly(repository);
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

    public String getBasePath(String configName) {
        String key = Comments.REPOSITORY_PREFIX + configName + ".base.path";
        String basePath = propertyResolver.getProperty(key);
        if (basePath == null) {
            basePath = propertyResolver.getProperty(defaultBasePathConfig);
        }
        if (basePath == null) {
            throw new IllegalArgumentException("Property " + key + " is absent");
        }
        return basePath.isEmpty() || basePath.endsWith("/") ? basePath : basePath + "/";
    }
}
