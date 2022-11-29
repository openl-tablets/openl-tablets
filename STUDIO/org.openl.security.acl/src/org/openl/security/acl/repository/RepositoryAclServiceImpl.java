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
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

public class RepositoryAclServiceImpl extends SimpleRepositoryAclServiceImpl implements RepositoryAclService {

    public RepositoryAclServiceImpl(MutableAclService aclService, String rootId, Class<?> objectIdentityClass) {
        super(aclService, rootId, objectIdentityClass);
    }

    private String buildObjectIdentityId(AProjectArtefact artefact) {
        if (artefact.getRepository() instanceof LocalRepository) {
            LocalRepository localRepository = (LocalRepository) artefact.getRepository();
            ProjectState projectState = localRepository.getProjectState(artefact.getProject().getFileData().getName());
            FileData fileData = projectState.getFileData();
            FileMappingData fileMappingData = fileData.getAdditionalData(FileMappingData.class);
            String repoPath = fileMappingData.getInternalPath();
            return concat(artefact.getRepository().getId(), repoPath + "/" + artefact.getInternalPath());
        } else {
            return concat(artefact.getRepository().getId(), artefact.getFileData().getName());
        }
    }

    private ObjectIdentity buildObjectIdentity(AProjectArtefact artefact) {
        return new ObjectIdentityImpl(getObjectIdentityClass(), buildObjectIdentityId(artefact));
    }

    public RepositoryAclServiceImpl(MutableAclService aclService,
            String rootId,
            Class<?> objectIdentityClass,
            Sid relevantSystemWideSid) {
        super(aclService, rootId, objectIdentityClass, relevantSystemWideSid);
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact artefact) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        return listPermissions(oi);
    }

    @Override
    @Transactional
    public void addPermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        addPermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void addPermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        addPermissions(oi, permissions);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact artefact) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        removePermissions(oi);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact artefact, List<Sid> sids) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        removePermissions(oi, sids);
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        removePermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removePermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        removePermissions(oi, permissions);
    }

    @Override
    @Transactional
    public void move(AProjectArtefact artefact, String newPath) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        moveInternal(artefact.getRepository().getName(), oi, newPath);
    }

    @Override
    @Transactional
    public boolean isGranted(AProjectArtefact artefact, List<Permission> permissions) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        if (LocalWorkspace.LOCAL_ID.equals(artefact.getRepository().getId())) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids = getSidRetrievalStrategy().getSids(authentication);
        ObjectIdentity oi = buildObjectIdentity(artefact);
        return isGranted(oi, sids, permissions);
    }

    @Override
    @Transactional
    public void deleteAcl(AProjectArtefact artefact) {
        Objects.requireNonNull(artefact, "artefact cannot be null");
        ObjectIdentity oi = buildObjectIdentity(artefact);
        aclService.deleteAcl(oi, true);
    }

    @Override
    @Transactional
    public boolean createAcl(AProjectArtefact artefact, List<Permission> permissions) {
        ObjectIdentity oi = buildObjectIdentity(artefact);
        return createAcl(oi, permissions);
    }
}
