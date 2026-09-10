package org.openl.security.acl.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.security.acl.MutableAclService;
import org.openl.security.acl.oid.AclObjectIdentityProvider;

public class RepositoryAclServiceImpl extends SimpleRepositoryAclServiceImpl implements RepositoryAclService {

    public RepositoryAclServiceImpl(AclCache springCacheBasedAclCache,
                                    Cache missingAclCache,
                                    MutableAclService aclService,
                                    Sid relevantSystemWideSid,
                                    SidRetrievalStrategy sidRetrievalStrategy,
                                    AclObjectIdentityProvider oidProvider) {
        super(springCacheBasedAclCache,
                missingAclCache,
                aclService,
                relevantSystemWideSid,
                sidRetrievalStrategy,
                oidProvider);
    }

    @Override
    @Transactional
    public void move(AProjectArtefact projectArtefact, String newPath) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var sids = sidRetrievalStrategy.getSids(authentication);
        var oi = oidProvider.getArtifactOid(projectArtefact);
        if (useParentStrategy) {
            oi = oidProvider.getParentOid(oi);
        }
        return isGranted(oi, sids, permissions);
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends AProjectArtefact> Set<T> filterGranted(Collection<T> artefacts, List<Permission> permissions) {
        Set<T> granted = Collections.newSetFromMap(new IdentityHashMap<>());
        if (artefacts.isEmpty()) {
            return granted;
        }
        var sids = sidRetrievalStrategy.getSids(SecurityContextHolder.getContext().getAuthentication());
        var decisions = new HashMap<ObjectIdentity, Boolean>();
        for (T artefact : artefacts) {
            if (isGrantedOnce(artefact, sids, permissions, decisions)) {
                granted.add(artefact);
            }
        }
        return granted;
    }

    /**
     * Answers for one artefact, reusing the answer already taken for its ACL identity.
     */
    private boolean isGrantedOnce(AProjectArtefact artefact,
                                  List<Sid> sids,
                                  List<Permission> permissions,
                                  Map<ObjectIdentity, Boolean> decisions) {
        if (artefact == null) {
            return false;
        }
        if (LocalWorkspace.LOCAL_ID.equals(artefact.getRepository().getId())) {
            return true;
        }
        return decisions.computeIfAbsent(oidProvider.getArtifactOid(artefact),
                oid -> isGranted(oid, sids, permissions));
    }

    @Override
    @Transactional
    public void deleteAcl(AProjectArtefact projectArtefact) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
        deleteAcl(oi);
    }

    @Override
    @Transactional
    public boolean createAcl(AProjectArtefact projectArtefact, List<Permission> permissions, boolean force) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
        return createAcl(oi, permissions, force);
    }

    @Override
    @Transactional
    public boolean hasAcl(AProjectArtefact projectArtefact) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
        return hasAcl(oi);
    }

    protected String cutRepositoryId(String identifier) {
        var d = identifier.indexOf(":");
        if (d >= 0) {
            return identifier.substring(d + 1);
        }
        return identifier;
    }

    @Override
    public String getPath(AProjectArtefact projectArtefact) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
        return cutRepositoryId((String) oi.getIdentifier());
    }

    @Override
    @Transactional
    public List<Permission> listPermissions(AProjectArtefact projectArtefact, Sid sid) {
        if (sid == null) {
            return List.of();
        }
        var oi = oidProvider.getArtifactOid(projectArtefact);
        var permissions = listPermissions(oi, List.of(sid));
        return permissions.getOrDefault(sid, List.of());
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact) {
        var oi = oidProvider.getArtifactOid(projectArtefact);
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
