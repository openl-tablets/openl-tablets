package org.openl.security.acl.repository;

import java.io.IOException;

import org.openl.rules.repository.api.FolderMapper;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.workspace.dtr.impl.MappedRepository;

public class SecureMappedRepository extends SecureBranchRepository implements FolderMapper {
    private final MappedRepository mappedRepository;

    public SecureMappedRepository(MappedRepository repository, SimpleRepositoryAclService simpleRepositoryAclService) {
        super(repository, simpleRepositoryAclService);
        this.mappedRepository = repository;
    }

    @Override
    public FolderRepository getDelegate() {
        return mappedRepository.getDelegate();
    }

    @Override
    public void addMapping(String internal) throws IOException {
        mappedRepository.addMapping(internal);
    }

    @Override
    public void removeMapping(String external) throws IOException {
        mappedRepository.removeMapping(external);
    }

    @Override
    public String getRealPath(String externalPath) {
        return mappedRepository.getRealPath(externalPath);
    }

    @Override
    public String getBusinessName(String mappedName) {
        return mappedRepository.getBusinessName(mappedName);
    }

    @Override
    public String getMappedName(String businessName, String path) {
        return mappedRepository.getMappedName(businessName, path);
    }

    @Override
    public String findMappedName(String internalPath) {
        return null;
    }
}
