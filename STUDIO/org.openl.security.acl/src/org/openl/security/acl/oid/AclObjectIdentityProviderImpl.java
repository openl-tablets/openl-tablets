package org.openl.security.acl.oid;

import java.util.Objects;

import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.security.acl.repository.Root;
import org.openl.security.acl.utils.AclPathUtils;

public class AclObjectIdentityProviderImpl implements AclObjectIdentityProvider {

    private final Class<?> oidClass;
    private final ObjectIdentity rootOid;

    public AclObjectIdentityProviderImpl(Class<?> oidClass, String rootId) {
        this.oidClass = oidClass;
        this.rootOid = new ObjectIdentityImpl(Root.class, rootId);
    }

    @Override
    public ObjectIdentity getRootOid() {
        return rootOid;
    }

    @Override
    public String getOidType() {
        return oidClass.getName();
    }

    @Override
    public ObjectIdentity getRepositoryOid(String repoId, String path) {
        Objects.requireNonNull(repoId, "repositoryId cannot be null");
        return new ObjectIdentityImpl(oidClass, AclPathUtils.buildRepositoryPath(repoId, path));
    }

    @Override
    public ObjectIdentity getArtifactOid(AProjectArtefact projectArtefact) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        var repoId = projectArtefact.getRepository().getId();
        var internalPath = AclPathUtils.extractInternalPath(projectArtefact);
        return getRepositoryOid(repoId, internalPath);
    }

    @Override
    public ObjectIdentity getParentOid(ObjectIdentity oid) {
        if (Root.class.getName().equals(oid.getType())) {
            return null;
        }
        var id = (String) oid.getIdentifier();
        int lastSlashIdx = id.lastIndexOf('/');
        if (lastSlashIdx < 0) {
            return getRootOid();
        }

        var parentOi = id.substring(0, lastSlashIdx);
        int lastChPos = parentOi.length() - 1;
        if (lastChPos >= 0 && parentOi.charAt(lastChPos) == ':') {
            parentOi = parentOi.substring(0, lastChPos);
        }
        return new ObjectIdentityImpl(oidClass, parentOi);
    }

    @Override
    public ObjectIdentity moveToNewParent(ObjectIdentity childOid, ObjectIdentity oldParentOid, ObjectIdentity newParentOid) {
        if (oldParentOid.equals(childOid)) {
            return newParentOid;
        }
        String newParentPath = ((String) newParentOid.getIdentifier());
        String oldParentPath = ((String) oldParentOid.getIdentifier());
        String fillOldChildPath = ((String) childOid.getIdentifier());

        String subChildPath = fillOldChildPath.substring(oldParentPath.length());
        var fullPath = newParentPath + subChildPath;
        return new ObjectIdentityImpl(oidClass, fullPath);
    }

}
