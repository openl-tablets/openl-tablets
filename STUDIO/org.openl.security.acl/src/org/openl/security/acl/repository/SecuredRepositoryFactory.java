package org.openl.security.acl.repository;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.impl.MappedRepository;

public final class SecuredRepositoryFactory {
    private SecuredRepositoryFactory() {
    }

    public static Repository wrapToSecureRepo(Repository repository,
                                              SimpleRepositoryAclService simpleRepositoryAclService) {
        if (repository == null) {
            return null;
        }
        if (repository instanceof MappedRepository mappedRepository) {
            return new SecureMappedRepository(mappedRepository, simpleRepositoryAclService);
        } else if (repository instanceof BranchRepository branchRepository) {
            return new SecureBranchRepository(branchRepository, simpleRepositoryAclService);
        } else {
            return new SecureRepository(repository, simpleRepositoryAclService);
        }
    }
}
