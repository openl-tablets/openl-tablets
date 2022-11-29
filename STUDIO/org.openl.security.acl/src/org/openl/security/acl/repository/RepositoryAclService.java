package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public interface RepositoryAclService extends SimpleRepositoryAclService {

    Map<Sid, List<Permission>> listPermissions(AProjectArtefact artefact);

    void addPermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids);

    void addPermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions);

    void move(AProjectArtefact artefact, String newPath);

    void deleteAcl(AProjectArtefact artefact);

    void removePermissions(AProjectArtefact artefact);

    void removePermissions(AProjectArtefact artefact, List<Sid> sids);

    void removePermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids);

    void removePermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions);

    boolean isGranted(AProjectArtefact artefact, List<Permission> permissions);

    boolean createAcl(AProjectArtefact artefact, List<Permission> permissions);
}
