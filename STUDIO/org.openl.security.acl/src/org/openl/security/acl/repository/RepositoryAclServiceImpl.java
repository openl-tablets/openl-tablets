package org.openl.security.acl.repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.security.acl.MutableAclService;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

public class RepositoryAclServiceImpl extends SimpleRepositoryAclServiceImpl implements RepositoryAclService {

    public RepositoryAclServiceImpl(SpringCacheBasedAclCache springCacheBasedAclCache,
            MutableAclService aclService,
            String rootId,
            Class<?> objectIdentityClass) {
        super(springCacheBasedAclCache, aclService, rootId, objectIdentityClass);
    }

    public RepositoryAclServiceImpl(SpringCacheBasedAclCache springCacheBasedAclCache,
            MutableAclService aclService,
            String rootId,
            Class<?> objectIdentityClass,
            Sid relevantSystemWideSid) {
        super(springCacheBasedAclCache, aclService, rootId, objectIdentityClass, relevantSystemWideSid);
    }

    private String buildObjectIdentityId(AProjectArtefact projectArtefact) {
        if (projectArtefact.getRepository() instanceof LocalRepository) {
            LocalRepository localRepository = (LocalRepository) projectArtefact.getRepository();
            ProjectState projectState = localRepository
                .getProjectState(projectArtefact.getProject().getFileData().getName());
            FileData fileData = projectState.getFileData();
            FileMappingData fileMappingData = fileData.getAdditionalData(FileMappingData.class);
            String repoPath = fileMappingData.getInternalPath();
            return concat(projectArtefact.getRepository().getId(), repoPath + "/" + projectArtefact.getInternalPath());
        } else {
            return concat(projectArtefact.getRepository().getId(), projectArtefact.getFileData().getName());
        }
    }

    private ObjectIdentity buildObjectIdentity(AProjectArtefact projectArtefact) {
        return new ObjectIdentityImpl(getObjectIdentityClass(), buildObjectIdentityId(projectArtefact));
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact projectArtefact) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        return listPermissions(oi);
    }

    @Override
    @Transactional
    public void addPermissions(AProjectArtefact projectArtefact, List<Permission> permissions, List<Sid> sids) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        addPermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void addPermissions(AProjectArtefact projectArtefact, Map<Sid, List<Permission>> permissions) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        addPermissions(oi, permissions);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        removePermissions(oi);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact, List<Sid> sids) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        removePermissions(oi, sids);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact, List<Permission> permissions, List<Sid> sids) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        removePermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact projectArtefact, Map<Sid, List<Permission>> permissions) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        removePermissions(oi, permissions);
    }

    @Override
    @Transactional
    public void move(AProjectArtefact projectArtefact, String newPath) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        moveInternal(projectArtefact.getRepository().getName(), oi, newPath);
    }

    @Override
    @Transactional
    public boolean isGranted(AProjectArtefact projectArtefact, List<Permission> permissions) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        if (LocalWorkspace.LOCAL_ID.equals(projectArtefact.getRepository().getId())) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids = getSidRetrievalStrategy().getSids(authentication);
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        return isGranted(oi, sids, permissions);
    }

    @Override
    @Transactional
    public void deleteAcl(AProjectArtefact projectArtefact) {
        Objects.requireNonNull(projectArtefact, "projectArtefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        aclService.deleteAcl(oi, true);
    }

    @Override
    @Transactional
    public boolean createAcl(AProjectArtefact projectArtefact, List<Permission> permissions) {
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        return createAcl(oi, permissions);
    }

    @Override
    @Transactional
    public Sid getOwner(AProjectArtefact projectArtefact) {
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        return getOwner(oi);
    }

    @Override
    @Transactional
    public boolean updateOwner(AProjectArtefact projectArtefact, Sid newOwner) {
        ObjectIdentity oi = buildObjectIdentity(projectArtefact);
        return updateOwner(oi, newOwner);
    }
}
