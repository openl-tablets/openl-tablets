package org.openl.security.acl.repository;

public class RepositoryAclServiceProviderImpl implements RepositoryAclServiceProvider {

    private final RepositoryAclService designRepositoryAclService;
    private final RepositoryAclService deployConfigRepositoryAclService;
    private final SimpleRepositoryAclService productionRepositoryAclService;

    public RepositoryAclServiceProviderImpl(RepositoryAclService designRepositoryAclService,
                                            RepositoryAclService deployConfigRepositoryAclService,
                                            SimpleRepositoryAclService productionRepositoryAclService) {
        this.designRepositoryAclService = designRepositoryAclService;
        this.deployConfigRepositoryAclService = deployConfigRepositoryAclService;
        this.productionRepositoryAclService = productionRepositoryAclService;
    }

    @Override
    public SimpleRepositoryAclService getAclService(String repositoryType) {
        switch (repositoryType) {
            case REPO_TYPE_PROD:
                return productionRepositoryAclService;
            case REPO_TYPE_DESIGN:
                return designRepositoryAclService;
            case REPO_TYPE_DEPLOY_CONFIG:
                return deployConfigRepositoryAclService;
            default:
                throw new IllegalArgumentException(String.format("Repository type '%s' is not found.", repositoryType));
        }
    }

    @Override
    public RepositoryAclService getDesignRepoAclService() {
        return designRepositoryAclService;
    }

    @Override
    public RepositoryAclService getDeployConfigRepoAclService() {
        return deployConfigRepositoryAclService;
    }

    @Override
    public SimpleRepositoryAclService getProdRepoAclService() {
        return productionRepositoryAclService;
    }
}
