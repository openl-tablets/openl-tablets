package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public interface SimpleRepositoryAclService {

    /**
     * Where a permission is held in the repositories of one kind.
     *
     * @param repositoryId the repository, or {@code null} for all the repositories of the kind
     * @param path         the path in the repository, or {@code null} for the repository itself
     */
    record Location(@Nullable String repositoryId, @Nullable String path) {

        /**
         * All the repositories of the kind.
         */
        public static final Location ROOT = new Location(null, null);
    }

    Map<Sid, List<Permission>> listPermissions(String repositoryId, String path);

    List<Permission> listPermissions(String repositoryId, String path, Sid sid);

    Map<Sid, List<Permission>> listRootPermissions();

    List<Permission> listRootPermissions(Sid sid);

    /**
     * The permissions the subject holds in the repositories of this kind: on all of them, on a repository and on a
     * path in one, by where they are held.
     */
    Map<Location, List<Permission>> listAllPermissions(Sid sid);

    void addPermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids);

    void addPermissions(String repositoryId, String path, Sid sid, Permission... permissions);

    void addRootPermissions(List<Permission> permissions, List<Sid> sids);

    void addRootPermissions(Sid sid, Permission... permissions);

    void move(String repositoryId, String path, String newPath);

    void deleteAcl(String repositoryId, String path);

    void removePermissions(String repositoryId, String path);

    void removePermissions(String repositoryId, String path, List<Sid> sids);

    void removePermissions(String repositoryId, String path, Sid sid);

    void removePermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids);

    void removeRootPermissions(List<Permission> permissions, List<Sid> sids);

    void removeRootPermissions(Sid sid);

    void removeRootPermissions();

    boolean isGranted(String repositoryId, String path, List<Permission> permissions);

    boolean isGranted(String repositoryId, String path, boolean useParentStrategy, Permission... permissions);

    boolean createAcl(String repositoryId, String path, List<Permission> permissions, boolean force);

    Sid getOwner(String repositoryId, String path);

    boolean updateOwner(String repositoryId, String path, Sid newOwner);
}
