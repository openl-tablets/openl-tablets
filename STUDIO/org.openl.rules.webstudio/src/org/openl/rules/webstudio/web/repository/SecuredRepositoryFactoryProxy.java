package org.openl.rules.webstudio.web.repository;

import lombok.RequiredArgsConstructor;

import org.openl.rules.repository.api.Repository;
import org.openl.security.acl.repository.SecuredRepositoryFactory;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

@RequiredArgsConstructor
public class SecuredRepositoryFactoryProxy implements RepositoryFactoryProxy {

    private final DefaultRepositoryFactoryProxy delegate;
    private final SimpleRepositoryAclService simpleRepositoryAclService;

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
