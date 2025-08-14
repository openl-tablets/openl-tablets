package org.openl.security.acl.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.acls.domain.PrincipalSid;
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
    public void move(AProjectArtefact projectArtefact, String newPath) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        moveInternal(projectArtefact.getRepository().getName(), oi, newPath);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGranted(AProjectArtefact projectArtefact, List<Permission> permissions) {
        return isGranted0(projectArtefact, false, permissions);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGranted(AProjectArtefact projectArtefact, boolean useParentStrategy, Permission... permissions) {
        return isGranted0(projectArtefact, useParentStrategy, List.of(permissions));
    }

    private boolean isGranted0(AProjectArtefact projectArtefact, boolean useParentStrategy, List<Permission> permissions) {
        if (projectArtefact == null) {
            return false;
        }
        if (LocalWorkspace.LOCAL_ID.equals(projectArtefact.getRepository().getId())) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids = sidRetrievalStrategy.getSids(authentication);
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        if (useParentStrategy) {
            oi = oidProvider.getParentOid(oi);
        }
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
    @Transactional
    public List<Permission> listPermissions(AProjectArtefact projectArtefact, Sid sid) {
        if (sid == null) {
            return Collections.emptyList();
        }
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        var permissions = listPermissions(oi, List.of(sid));
        return permissions.getOrDefault(sid, Collections.emptyList());
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = oidProvider.getArtifactOid(projectArtefact);
        return listPermissions(oi, null);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact, Sid sid) {
        if (sid == null) {
            return;
        }
        var oi = oidProvider.getArtifactOid(projectArtefact);
        removePermissions(oi, List.of(sid));
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
        removePermissions(oi);
    }

    @Override
    @Transactional
    public void addPermissions(AProjectArtefact projectArtefact, Sid sid, Permission... permissions) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
        addPermissions(oi, Map.of(sid, List.of(permissions)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(AProjectArtefact projectArtefact) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
        var owner = getOwner(oi);
        var sid = new PrincipalSid(SecurityContextHolder.getContext().getAuthentication());
        return Objects.equals(owner, sid);
    }
}
