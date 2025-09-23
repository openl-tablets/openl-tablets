package org.openl.rules.webstudio.web.repository;

import org.openl.rules.repository.api.Repository;

/**
 * Proxy interface for managing repository factory operations.
 * This interface provides methods for repository instance management,
 * configuration retrieval, and lifecycle operations.
 */
public interface RepositoryFactoryProxy {

    /**
     * Retrieves the repository list configuration.
     * 
     * @return the repository configuration as a string
     */
    String getRepoListConfig();

    /**
     * Creates or retrieves a repository instance for the specified configuration.
     * 
     * @param configName the name of the repository configuration
     * @return the repository instance associated with the given configuration
     */
    Repository getRepositoryInstance(String configName);

    /**
     * Releases resources associated with a repository configuration.
     * 
     * @param configName the name of the repository configuration to release
     */
    void releaseRepository(String configName);

    /**
     * Destroys the factory proxy and releases all associated resources.
     * Should be called when the proxy is no longer needed.
     */
    void destroy();

    /**
     * Retrieves the base path for a repository configuration.
     * 
     * @param configName the name of the repository configuration
     * @return the base path associated with the configuration
     */
    String getBasePath(String configName);

}
