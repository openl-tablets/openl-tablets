package org.openl.security.acl.repository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RepositoryAclServiceProviderImpl implements RepositoryAclServiceProvider {

    private final RepositoryAclService designRepositoryAclService;
    private final SimpleRepositoryAclService productionRepositoryAclService;

    @Override
    public SimpleRepositoryAclService getAclService(String repositoryType) {
        return switch (repositoryType) {
            case REPO_TYPE_PROD -> productionRepositoryAclService;
            case REPO_TYPE_DESIGN -> designRepositoryAclService;
            default ->
                    throw new IllegalArgumentException("Repository type '%s' is not found.".formatted(repositoryType));
        };
    }

    @Override
    public RepositoryAclService getDesignRepoAclService() {
        return designRepositoryAclService;
    }

    @Override
    public SimpleRepositoryAclService getProdRepoAclService() {
        return productionRepositoryAclService;
    }

}
