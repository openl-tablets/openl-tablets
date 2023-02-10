package org.openl.security.acl.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public class DisabledRepositoryAclServiceImpl extends DisabledSimpleRepositoryAclServiceImpl implements RepositoryAclService {

    @Override
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact artefact) {
        return Collections.emptyMap();
    }

    @Override
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact, List<Sid> sids) {
        return Collections.emptyMap();
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
    public boolean createAcl(AProjectArtefact artefact, List<Permission> permissions, boolean force) {
        return true;
    }

    @Override
    public Sid getOwner(AProjectArtefact projectArtefact) {
        return null;
    }

    @Override
    public boolean updateOwner(AProjectArtefact projectArtefact, Sid newOwner) {
        return true;
    }
}
