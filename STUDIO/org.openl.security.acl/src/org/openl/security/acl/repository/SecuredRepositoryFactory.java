package org.openl.security.acl.repository;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.impl.MappedRepository;

public final class SecuredRepositoryFactory {
    private SecuredRepositoryFactory() {
    }

    public static Repository wrapToSecureRepo(Repository repository, RepositoryAclService repositoryAclService) {
        if (repository == null) {
            return null;
        }
        if (repository instanceof MappedRepository) {
            return new SecureMappedRepository((MappedRepository) repository, repositoryAclService);
        } else if (repository instanceof BranchRepository) {
            return new SecureBranchRepository((BranchRepository) repository, repositoryAclService);
        } else if (repository instanceof FolderRepository) {
            return new SecureFolderRepository((FolderRepository) repository, repositoryAclService);
        } else {
            return new SecureRepository(repository, repositoryAclService);
        }
    }
}
