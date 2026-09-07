package org.openl.security.acl.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import org.openl.rules.project.abstraction.AProjectArtefact;

public interface RepositoryAclService extends SimpleRepositoryAclService {

    void move(AProjectArtefact projectArtefact, String newPath);

    void deleteAcl(AProjectArtefact projectArtefact);

    boolean isGranted(AProjectArtefact projectArtefact, List<Permission> permissions);

    boolean isGranted(AProjectArtefact projectArtefact, boolean useParentStrategy, Permission... permissions);

    /**
     * Answers one permission question for every given artefact at once.
     *
     * <p>An ACL identity is a repository and an internal path, so artefacts that address the same
     * identity share one answer and are evaluated once. Every branch that keeps a project in the same
     * folder addresses the same identity.
     *
     * <p>The whole batch is read in one transaction.
     *
     * @return the artefacts the current user holds the permissions on, compared by reference
     */
    <T extends AProjectArtefact> Set<T> filterGranted(Collection<T> artefacts, List<Permission> permissions);

    boolean createAcl(AProjectArtefact projectArtefact, List<Permission> permissions, boolean force);

    boolean hasAcl(AProjectArtefact projectArtefact);

    String getPath(AProjectArtefact projectArtefact);

    List<Permission> listPermissions(AProjectArtefact projectArtefact, Sid sid);

    Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact);

    void removePermissions(AProjectArtefact projectArtefact, Sid sid);

    void removePermissions(AProjectArtefact projectArtefact);

    void addPermissions(AProjectArtefact projectArtefact, Sid sid, Permission... permissions);

    /**
     * Check if the current user is the owner of the project artefact.
     *
     * @param projectArtefact project artefact
     * @return {@code true} if the current user is the owner of the project artefact
     */
    boolean isOwner(AProjectArtefact projectArtefact);
}
