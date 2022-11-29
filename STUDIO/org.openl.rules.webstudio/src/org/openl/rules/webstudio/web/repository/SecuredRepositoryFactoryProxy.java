package org.openl.rules.webstudio.web.repository;

import org.openl.rules.repository.api.Repository;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SecuredRepositoryFactory;

public class SecuredRepositoryFactoryProxy extends RepositoryFactoryProxy {
    private final RepositoryFactoryProxy delegate;
    private final RepositoryAclService repositoryAclService;

    public SecuredRepositoryFactoryProxy(RepositoryFactoryProxy delegate, RepositoryAclService repositoryAclService) {
        this.delegate = delegate;
        this.repositoryAclService = repositoryAclService;
    }

    @Override
    public String getRepoListConfig() {
        return delegate.getRepoListConfig();
    }

    @Override
    public Repository getRepositoryInstance(String configName) {
        return SecuredRepositoryFactory.wrapToSecureRepo(delegate.getRepositoryInstance(configName),
            repositoryAclService);
    }

    @Override
    public void releaseRepository(String configName) {
        delegate.releaseRepository(configName);
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    @Override
    public String getBasePath(String configName) {
        return delegate.getBasePath(configName);
    }
}
