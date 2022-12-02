package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public interface RepositoryAclService extends SimpleRepositoryAclService {

    Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact);

    void addPermissions(AProjectArtefact projectArtefact, List<Permission> permissions, List<Sid> sids);

    void addPermissions(AProjectArtefact projectArtefact, Map<Sid, List<Permission>> permissions);

    void move(AProjectArtefact projectArtefact, String newPath);

    void deleteAcl(AProjectArtefact projectArtefact);

    void removePermissions(AProjectArtefact projectArtefact);

    void removePermissions(AProjectArtefact projectArtefact, List<Sid> sids);

    void removePermissions(AProjectArtefact projectArtefact, List<Permission> permissions, List<Sid> sids);

    void removePermissions(AProjectArtefact v, Map<Sid, List<Permission>> permissions);

    boolean isGranted(AProjectArtefact projectArtefact, List<Permission> permissions);

    boolean createAcl(AProjectArtefact projectArtefact, List<Permission> permissions);

    Sid getOwner(AProjectArtefact projectArtefact);

    boolean updateOwner(AProjectArtefact projectArtefact, Sid newOwner);
}
