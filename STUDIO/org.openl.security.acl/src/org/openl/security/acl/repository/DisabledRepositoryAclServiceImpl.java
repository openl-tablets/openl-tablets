package org.openl.security.acl.repository;

import java.util.List;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.project.abstraction.AProjectArtefact;

public class DisabledRepositoryAclServiceImpl extends DisabledSimpleRepositoryAclServiceImpl implements RepositoryAclService {

    @Override
    public void move(AProjectArtefact artefact, String newPath) {
    }

    @Override
    public void deleteAcl(AProjectArtefact artefact) {
    }

    @Override
    public boolean isGranted(AProjectArtefact artefact, List<Permission> permissions) {
        return true;
    }

    @Override
    public boolean createAcl(AProjectArtefact artefact, List<Permission> permissions, boolean force) {
        return true;
    }

    @Override
    public boolean hasAcl(AProjectArtefact projectArtefact) {
        return true;
    }

    @Override
    public String getPath(AProjectArtefact projectArtefact) {
        return null;
    }

    @Override
    public String getFullPath(AProjectArtefact projectArtefact) {
        return null;
    }
}
