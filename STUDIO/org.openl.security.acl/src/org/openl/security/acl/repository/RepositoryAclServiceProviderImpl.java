package org.openl.security.acl.repository;

public class RepositoryAclServiceProviderImpl implements RepositoryAclServiceProvider {

    private final RepositoryAclService designRepositoryAclService;
    private final SimpleRepositoryAclService productionRepositoryAclService;

    public RepositoryAclServiceProviderImpl(RepositoryAclService designRepositoryAclService,
                                            SimpleRepositoryAclService productionRepositoryAclService) {
        this.designRepositoryAclService = designRepositoryAclService;
        this.productionRepositoryAclService = productionRepositoryAclService;
    }

    @Override
    public SimpleRepositoryAclService getAclService(String repositoryType) {
        return switch (repositoryType) {
            case REPO_TYPE_PROD -> productionRepositoryAclService;
            case REPO_TYPE_DESIGN -> designRepositoryAclService;
            default ->
                    throw new IllegalArgumentException(String.format("Repository type '%s' is not found.", repositoryType));
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
