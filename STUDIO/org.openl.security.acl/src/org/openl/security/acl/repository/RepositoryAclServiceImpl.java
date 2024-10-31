package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;

import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.security.acl.MutableAclService;
import org.openl.security.acl.oid.AclObjectIdentityProvider;

public class RepositoryAclServiceImpl extends SimpleRepositoryAclServiceImpl implements RepositoryAclService {

    public RepositoryAclServiceImpl(AclCache springCacheBasedAclCache,
                                    MutableAclService aclService,
                                    Sid relevantSystemWideSid,
                                    SidRetrievalStrategy sidRetrievalStrategy,
                                    AclObjectIdentityProvider oidProvider) {
        super(springCacheBasedAclCache,
                aclService,
                relevantSystemWideSid,
                sidRetrievalStrategy,
                oidProvider);
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return listPermissions(oi, null);
    }

    @Override
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact, List<Sid> sids) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return listPermissions(oi, sids);
    }

    @Override
    @Transactional
    public void addPermissions(AProjectArtefact projectArtefact, List<Permission> permissions, List<Sid> sids) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        addPermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void addPermissions(AProjectArtefact projectArtefact, Map<Sid, List<Permission>> permissions) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        addPermissions(oi, permissions);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        removePermissions(oi);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact, List<Sid> sids) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        removePermissions(oi, sids);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact, List<Permission> permissions, List<Sid> sids) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        removePermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact, Map<Sid, List<Permission>> permissions) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        removePermissions(oi, permissions);
    }

    @Override
    @Transactional
    public void move(AProjectArtefact projectArtefact, String newPath) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        moveInternal(projectArtefact.getRepository().getName(), oi, newPath);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGranted(AProjectArtefact projectArtefact, List<Permission> permissions) {
        if (projectArtefact == null) {
            return false;
        }
        if (LocalWorkspace.LOCAL_ID.equals(projectArtefact.getRepository().getId())) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids = sidRetrievalStrategy.getSids(authentication);
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return isGranted(oi, sids, permissions);
    }

    @Override
    @Transactional
    public void deleteAcl(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        aclService.deleteAcl(oi, true);
    }

    @Override
    @Transactional
    public boolean createAcl(AProjectArtefact projectArtefact, List<Permission> permissions, boolean force) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return createAcl(oi, permissions, force);
    }

    @Override
    @Transactional
    public boolean hasAcl(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return hasAcl(oi);
    }

    @Override
    @Transactional
    public Sid getOwner(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return getOwner(oi);
    }

    @Override
    @Transactional
    public boolean updateOwner(AProjectArtefact projectArtefact, Sid newOwner) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return updateOwner(oi, newOwner);
    }

    protected String cutRepositoryId(String identifier) {
        int d = identifier.indexOf(":");
        if (d >= 0) {
            return identifier.substring(d + 1);
        }
        return identifier;
    }

    @Override
    public String getPath(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return cutRepositoryId((String) oi.getIdentifier());
    }

    @Override
    public String getFullPath(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return (String) oi.getIdentifier();
    }
}
