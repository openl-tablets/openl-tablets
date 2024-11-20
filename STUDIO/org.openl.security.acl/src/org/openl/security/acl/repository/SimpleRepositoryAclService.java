package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public interface SimpleRepositoryAclService {

    Map<Sid, List<Permission>> listPermissions(String repositoryId, String path);

    Map<Sid, List<Permission>> listRootPermissions();

    List<Permission> listRootPermissions(Sid sid);

    void addPermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids);

    void addRootPermissions(List<Permission> permissions, List<Sid> sids);

    void addRootPermissions(Sid sid, Permission... permissions);

    void move(String repositoryId, String path, String newPath);

    void deleteAcl(String repositoryId, String path);

    void removePermissions(String repositoryId, String path);

    void removePermissions(String repositoryId, String path, List<Sid> sids);

    void removePermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids);

    void removeRootPermissions(List<Permission> permissions, List<Sid> sids);

    void removeRootPermissions(Sid sid);

    void removeRootPermissions();

    boolean isGranted(String repositoryId, String path, List<Permission> permissions);

    boolean createAcl(String repositoryId, String path, List<Permission> permissions, boolean force);

    Sid getOwner(String repositoryId, String path);

    boolean updateOwner(String repositoryId, String path, Sid newOwner);
}
