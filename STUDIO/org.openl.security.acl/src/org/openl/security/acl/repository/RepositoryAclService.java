package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public interface RepositoryAclService {

    Map<Sid, List<Permission>> listPermissions(String repositoryId, String path);

    Map<Sid, List<Permission>> listPermissions(AProjectArtefact artefact);

    Map<Sid, List<Permission>> listRootPermissions();

    void addPermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions);

    void addPermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids);

    void addPermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids);

    void addPermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions);

    void addRootPermissions(Map<Sid, List<Permission>> permissions);

    void addRootPermissions(List<Permission> permissions, List<Sid> sids);

    void move(AProjectArtefact artefact, String newPath);

    void move(String repositoryId, String path, String newPath);

    void deleteAcl(AProjectArtefact artefact);

    void deleteAcl(String repositoryId, String path);

    void deleteAclRoot();

    void removePermissions(AProjectArtefact artefact);

    void removePermissions(String repositoryId, String path);

    void removePermissions(AProjectArtefact artefact, List<Sid> sids);

    void removePermissions(String repositoryId, String path, List<Sid> sids);

    void removePermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids);

    void removePermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions);

    void removePermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids);

    void removePermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions);

    void removeRootPermissions(List<Permission> permissions, List<Sid> sids);

    void removeRootPermissions(List<Sid> sids);

    void removeRootPermissions();

    boolean isGranted(String repositoryId, String path, List<Permission> permissions);

    boolean isGranted(AProjectArtefact artefact, List<Permission> permissions);

    boolean createAcl(String repositoryId, String path, List<Permission> permissions);

    boolean createAcl(AProjectArtefact artefact, List<Permission> permissions);

    void deleteSid(Sid sid);
}
