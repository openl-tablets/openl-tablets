package org.openl.security.acl.repository;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.impl.MappedRepository;

public final class SecuredRepositoryFactory {
    private SecuredRepositoryFactory() {
    }

    public static Repository wrapToSecureRepo(Repository repository,
                                              SimpleRepositoryAclService simpleRepositoryAclService) {
        return switch (repository) {
            case null -> null;
            case MappedRepository mappedRepository ->
                    new SecureMappedRepository(mappedRepository, simpleRepositoryAclService);
            case BranchRepository branchRepository ->
                    new SecureBranchRepository(branchRepository, simpleRepositoryAclService);
            default -> new SecureRepository(repository, simpleRepositoryAclService);
        };
    }
}
