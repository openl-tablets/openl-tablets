package org.openl.security.acl.repository;

import java.util.List;

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
