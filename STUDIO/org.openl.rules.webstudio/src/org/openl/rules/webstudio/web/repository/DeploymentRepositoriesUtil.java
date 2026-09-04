package org.openl.rules.webstudio.web.repository;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;

public class DeploymentRepositoriesUtil {
    private DeploymentRepositoriesUtil() {
    }

    public static boolean isMainBranchProtected(Repository repo) {
        if (repo.supports().branches()) {
            var branchRepo = (BranchRepository) repo;
            return branchRepo.isBranchProtected(branchRepo.getBranch());
        }
        return false;
    }
}
