package org.openl.security.acl.oid;

import org.springframework.security.acls.model.ObjectIdentity;

import org.openl.rules.project.abstraction.AProjectArtefact;

public interface AclObjectIdentityProvider {

    /**
     * Get root object identity
     *
     * @return root object identity
     */
    ObjectIdentity getRootOid();

    /**
     * Get object identity for repository path
     *
     * @param repoId repository id
     * @param path   path
     * @return repository object identity
     */
    ObjectIdentity getRepositoryOid(String repoId, String path);

    /**
     * Get object identity for project artefact
     *
     * @param projectArtefact project artefact
     * @return artifact object identity
     */
    ObjectIdentity getArtifactOid(AProjectArtefact projectArtefact);

    /**
     * Get parent object identity
     *
     * @param oid object identity
     * @return parent object identity
     */
    ObjectIdentity getParentOid(ObjectIdentity oid);

    /**
     * Move child object identity form old parent to new parent
     *
     * @param childOid     child object identity to move
     * @param oldParentOid old parent object identity
     * @param newParentOid new parent object identity
     * @return moved object identity
     */
    ObjectIdentity moveToNewParent(ObjectIdentity childOid, ObjectIdentity oldParentOid, ObjectIdentity newParentOid);

    /**
     * Get object identity type
     *
     * @return object identity type
     */
    String getOidType();

}
