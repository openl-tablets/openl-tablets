package org.openl.rules.webstudio.web.repository;

import org.openl.rules.repository.api.Repository;
import org.openl.security.acl.repository.SecuredRepositoryFactory;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

public class SecuredRepositoryFactoryProxy extends RepositoryFactoryProxy {
    private final RepositoryFactoryProxy delegate;
    private final SimpleRepositoryAclService simpleRepositoryAclService;

    public SecuredRepositoryFactoryProxy(RepositoryFactoryProxy delegate,
                                         SimpleRepositoryAclService simpleRepositoryAclService) {
        this.delegate = delegate;
        this.simpleRepositoryAclService = simpleRepositoryAclService;
    }

    @Override
    public String getRepoListConfig() {
        return delegate.getRepoListConfig();
    }

    @Override
    public Repository getRepositoryInstance(String configName) {
        return SecuredRepositoryFactory.wrapToSecureRepo(delegate.getRepositoryInstance(configName),
                simpleRepositoryAclService);
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
