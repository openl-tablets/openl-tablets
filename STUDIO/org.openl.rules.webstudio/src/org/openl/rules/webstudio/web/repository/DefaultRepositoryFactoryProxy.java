package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.DESIGN_REPOSITORY_CONFIGS;
import static org.openl.rules.webstudio.web.admin.AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.util.IOUtils;

/**
 * Repository Factory Proxy.
 * <p/>
 * Takes actual factory description from the environment in which the current application is running.
 */
public class DefaultRepositoryFactoryProxy implements RepositoryFactoryProxy {

    private final static String REPOSITORY_DEFAULT_BASE_PATH_TEMPLATE = RepositoryConfiguration.REPOSITORY_DEFAULT_PREFIX + "%s" + RepositorySettings.BASE_PATH_SUFFIX;

    /**
     * How long a repository that could not be created is left alone before it is tried again.
     *
     * <p>Reconfiguring a repository restarts the application, which forgets every refusal at once, so this
     * window only decides how soon a repository that recovered on its own is picked up.
     */
    private static final long RETRY_REFUSED_AFTER = TimeUnit.MINUTES.toMillis(1);

    private final Map<String, Repository> factories = new ConcurrentHashMap<>();

    /**
     * The repositories that could not be created, and what refused them.
     *
     * <p>Creating one reaches its storage, and an unreachable or misconfigured repository answers only when
     * its connection gives up. Without this, every caller paid that wait again: the project list asks whether
     * the user may deploy anywhere, so a single broken production repository taxed every listing.
     */
    private final Map<String, Refusal> refusals = new ConcurrentHashMap<>();

    private final PropertyResolver propertyResolver;
    @Getter
    private final String repoListConfig;
    private final String defaultBasePathConfig;

    public DefaultRepositoryFactoryProxy(PropertyResolver propertyResolver, RepositoryMode mode) {
        switch (mode) {
            case DESIGN:
                repoListConfig = DESIGN_REPOSITORY_CONFIGS;
                break;
            case PRODUCTION:
                repoListConfig = PRODUCTION_REPOSITORY_CONFIGS;
                break;
            default:
                throw new IllegalArgumentException("Repository mode " + mode + " is not supported");
        }
        this.propertyResolver = propertyResolver;
        this.defaultBasePathConfig = REPOSITORY_DEFAULT_BASE_PATH_TEMPLATE.formatted(mode.name().toLowerCase());
    }

    @Override
    public Repository getRepositoryInstance(String configName) {
        Objects.requireNonNull(configName);
        var repository = factories.get(configName);
        if (repository != null) {
            return repository;
        }
        // Both answers are read before the monitor is taken, so a caller of a repository already known to
        // refuse is not left waiting behind another one that is still reaching its storage.
        var refusal = standingRefusal(configName);
        if (refusal != null) {
            throw refusal;
        }
        synchronized (this) {
            repository = factories.get(configName);
            if (repository != null) {
                return repository;
            }
            refusal = standingRefusal(configName);
            if (refusal != null) {
                throw refusal;
            }
            try {
                repository = RepositoryInstatiator.newRepository(Comments.REPOSITORY_PREFIX + configName,
                        propertyResolver::getProperty);
            } catch (RuntimeException e) {
                refusals.put(configName, new Refusal(System.currentTimeMillis(), configName, e));
                throw e;
            }
            refusals.remove(configName);
            factories.put(configName, repository);
            return repository;
        }
    }

    /**
     * The refusal a repository still stands by, as an exception raised here rather than the one raised when it
     * was first reached: that one belongs to another caller and says nothing about this one.
     */
    private @Nullable RuntimeException standingRefusal(String configName) {
        var refusal = refusals.get(configName);
        return refusal == null || !refusal.stands(System.currentTimeMillis()) ? null : refusal.repeat();
    }

    /**
     * A repository that could not be created, and when that was last tried.
     */
    private record Refusal(long at, String configName, RuntimeException cause) {
        boolean stands(long now) {
            return now - at < RETRY_REFUSED_AFTER;
        }

        RuntimeException repeat() {
            return new IllegalStateException(
                    "Repository '%s' could not be created and is not reached again yet.".formatted(configName),
                    cause);
        }
    }

    @Override
    public void releaseRepository(String configName) {
        synchronized (this) {
            refusals.remove(configName);
            var repository = factories.remove(configName);
            if (repository != null) {
                // Close repo connection after validation
                IOUtils.closeQuietly(repository);
            }
        }
    }

    @Override
    public void destroy() {
        synchronized (this) {
            for (Repository repository : factories.values()) {
                // Close repo connection after validation
                IOUtils.closeQuietly(repository);
            }
            factories.clear();
            refusals.clear();
        }
    }

    @Override
    public String getBasePath(String configName) {
        var key = Comments.REPOSITORY_PREFIX + configName + RepositorySettings.BASE_PATH_SUFFIX;
        var basePath = propertyResolver.getProperty(key);
        if (basePath == null) {
            basePath = propertyResolver.getProperty(defaultBasePathConfig);
        }
        if (basePath == null) {
            throw new IllegalArgumentException("Property " + key + " is absent");
        }
        return basePath.isEmpty() || basePath.endsWith("/") ? basePath : basePath + "/";
    }
}
