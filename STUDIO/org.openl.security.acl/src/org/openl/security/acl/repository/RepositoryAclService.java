package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import org.openl.rules.project.abstraction.AProjectArtefact;

public interface RepositoryAclService extends SimpleRepositoryAclService {

    void move(AProjectArtefact projectArtefact, String newPath);

    void deleteAcl(AProjectArtefact projectArtefact);

    boolean isGranted(AProjectArtefact projectArtefact, List<Permission> permissions);

    boolean createAcl(AProjectArtefact projectArtefact, List<Permission> permissions, boolean force);

    boolean hasAcl(AProjectArtefact projectArtefact);

    String getPath(AProjectArtefact projectArtefact);

    String getFullPath(AProjectArtefact projectArtefact);

    List<Permission> listPermissions(AProjectArtefact projectArtefact, Sid sid);

    Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact);

    void removePermissions(AProjectArtefact projectArtefact, Sid sid);

    void addPermissions(AProjectArtefact projectArtefact, Sid sid, Permission... permissions);
}
