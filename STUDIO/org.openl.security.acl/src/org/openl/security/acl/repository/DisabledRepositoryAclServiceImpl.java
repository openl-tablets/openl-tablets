package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public class DisabledRepositoryAclServiceImpl extends DisabledSimpleRepositoryAclServiceImpl implements RepositoryAclService {

    @Override
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact artefact) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void addPermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions) {
    }

    @Override
    public void move(AProjectArtefact artefact, String newPath) {
    }

    @Override
    public void deleteAcl(AProjectArtefact artefact) {
    }

    @Override
    public void removePermissions(AProjectArtefact artefact) {
    }

    @Override
    public void removePermissions(AProjectArtefact artefact, List<Sid> sids) {
    }

    @Override
    public void removePermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void removePermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions) {
    }

    @Override
    public boolean isGranted(AProjectArtefact artefact, List<Permission> permissions) {
        return true;
    }

    @Override
    public boolean createAcl(AProjectArtefact artefact, List<Permission> permissions) {
        return true;
    }
}
