package org.openl.security.acl.repository;

public interface RepositoryAclServiceProvider {

    String REPO_TYPE_PROD = "prod";
    String REPO_TYPE_DEPLOY_CONFIG = "deployConfig";
    String REPO_TYPE_DESIGN = "design";

    /**
     * Get ACL service by repository type
     *
     * @param repositoryType repository type
     * @return ACL service
     */
    SimpleRepositoryAclService getAclService(String repositoryType);

    /**
     * Get design repository ACL service
     *
     * @return design repository ACL service
     */
    RepositoryAclService getDesignRepoAclService();

    /**
     * Get deploy config repository ACL service
     *
     * @return deploy config repository ACL service
     */
    RepositoryAclService getDeployConfigRepoAclService();

    /**
     * Get production repository ACL service
     *
     * @return production repository ACL service
     */
    SimpleRepositoryAclService getProdRepoAclService();
}
