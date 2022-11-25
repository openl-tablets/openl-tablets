package org.openl.security.acl.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.security.acl.MutableAclService;
import org.openl.util.StringUtils;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

public class RepositoryAclServiceImpl implements RepositoryAclService {

    private final MutableAclService aclService;
    private Sid relevantSystemWideSid;

    private SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

    public RepositoryAclServiceImpl(MutableAclService aclService) {
        this.aclService = aclService;
    }

    public static String concatPaths(String path1, String path2) {
        if (!path1.endsWith("/")) {
            path1 = path1 + "/";
        }
        return path1 + path2;
    }

    static String concat(String repositoryId, String path) {
        if (path != null) {
            String[] parts = path.split("/");
            path = Arrays.stream(parts).map(e -> e.trim() + "/").collect(Collectors.joining());
            while (path.contains("//")) {
                path = path.replaceAll("//", "/");
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
        }
        return StringUtils.isNotBlank(path) ? repositoryId + ":" + path : repositoryId;
    }

    public RepositoryAclServiceImpl(MutableAclService aclService, Sid relevantSystemWideSid) {
        this(aclService);
        this.relevantSystemWideSid = relevantSystemWideSid;
    }

    public SidRetrievalStrategy getSidRetrievalStrategy() {
        return sidRetrievalStrategy;
    }

    public void setSidRetrievalStrategy(SidRetrievalStrategy sidRetrievalStrategy) {
        this.sidRetrievalStrategy = sidRetrievalStrategy;
    }

    static String buildObjectIdentityId(AProjectArtefact artefact) {
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

    static ObjectIdentity buildObjectIdentity(AProjectArtefact artefact) {
        return new ObjectIdentityImpl(ProjectArtifact.class, buildObjectIdentityId(artefact));
    }

    static ObjectIdentity buildParentObjectIdentity(ObjectIdentity oi) {
        if (Root.class.getName().equals(oi.getType())) {
            return null;
        }
        String id = (String) oi.getIdentifier();
        int i = id.lastIndexOf("/");
        if (i >= 0) {
            String parentObjectIdentity = id.substring(0, i);
            if (parentObjectIdentity.endsWith(":")) {
                parentObjectIdentity = parentObjectIdentity.substring(0, parentObjectIdentity.length() - 1);
            }
            return new ObjectIdentityImpl(ProjectArtifact.class, parentObjectIdentity);
        }
        return getRootObjectIdentity();
    }

    public static ObjectIdentity getRootObjectIdentity() {
        return new ObjectIdentityImpl(Root.class, "1");
    }

    private MutableAcl getOrCreateAcl(ObjectIdentity oi) {
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
            acl.setEntriesInheriting(true);
            ObjectIdentity poi = buildParentObjectIdentity(oi);
            if (poi != null) {
                MutableAcl pAcl = getOrCreateAcl(poi);
                acl.setParent(pAcl);
                acl.setOwner(pAcl.getOwner());
            } else {
                if (relevantSystemWideSid != null) {
                    acl.setOwner(relevantSystemWideSid);
                }
            }
            aclService.updateAcl(acl);
        }
        return acl;
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listPermissions(String repositoryId, String path) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        return listPermissions(oi);
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
    public Map<Sid, List<Permission>> listRootPermissions() {
        return listPermissions(getRootObjectIdentity());
    }

    @Override
    @Transactional
    public void addPermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        addPermissions(oi, permissions);
    }

    private Map<Sid, List<Permission>> listPermissions(ObjectIdentity objectIdentity) {
        try {
            Map<Sid, List<Permission>> map = new HashMap<>();
            Acl acl = aclService.readAclById(objectIdentity);
            for (AccessControlEntry ace : acl.getEntries()) {
                if (ace.isGranting()) {
                    List<Permission> p = map.computeIfAbsent(ace.getSid(), k -> new ArrayList<>());
                    p.add(ace.getPermission());
                }
            }
            return map;
        } catch (NotFoundException e) {
            return Collections.emptyMap();
        }
    }

    private void addPermissions(ObjectIdentity objectIdentity, Map<Sid, List<Permission>> permissions) {
        if (!(Objects.equals(objectIdentity.getType(),
            ProjectArtifact.class.getName()) || (Objects.equals(objectIdentity.getType(), Root.class.getName())))) {
            throw new IllegalArgumentException("Invalid object identity");
        }
        if (permissions == null) {
            return;
        }
        MutableAcl acl = getOrCreateAcl(objectIdentity);
        Map<Sid, LinkedHashSet<Permission>> existingPermissions = new HashMap<>();
        for (int i = 0; i < acl.getEntries().size(); i++) {
            AccessControlEntry ace = acl.getEntries().get(i);
            if (ace.isGranting()) {
                LinkedHashSet<Permission> p = existingPermissions.computeIfAbsent(ace.getSid(),
                    k -> new LinkedHashSet<>());
                p.add(ace.getPermission());
            }
        }
        for (Map.Entry<Sid, List<Permission>> entry : permissions.entrySet()) {
            if (entry.getKey() != null) {
                Sid sid = entry.getKey();
                LinkedHashSet<Permission> uniquePermissions = new LinkedHashSet<>(entry.getValue());
                LinkedHashSet<Permission> p = existingPermissions.get(sid);
                for (Permission permission : uniquePermissions) {
                    if (p == null || !p.contains(permission)) {
                        acl.insertAce(acl.getEntries().size(), permission, sid, true);
                    }
                }
            }
        }
        aclService.updateAcl(acl);
    }

    private Map<Sid, List<Permission>> joinSidsAndPermissions(List<Permission> permissions, List<Sid> sids) {
        Map<Sid, List<Permission>> ret = new HashMap<>();
        for (Sid sid : sids) {
            ret.put(sid, permissions);
        }
        return ret;
    }

    @Override
    @Transactional
    public void addPermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        addPermissions(oi, joinSidsAndPermissions(permissions, sids));
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
    public void addRootPermissions(Map<Sid, List<Permission>> permissions) {
        addPermissions(getRootObjectIdentity(), permissions);
    }

    @Override
    @Transactional
    public void addRootPermissions(List<Permission> permissions, List<Sid> sids) {
        addPermissions(getRootObjectIdentity(), joinSidsAndPermissions(permissions, sids));
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
    public void removePermissions(String repositoryId, String path) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        removePermissions(oi);
    }

    private void removePermissions(ObjectIdentity objectIdentity) {
        if (!(Objects.equals(objectIdentity.getType(),
            ProjectArtifact.class.getName()) || (Objects.equals(objectIdentity.getType(), Root.class.getName())))) {
            throw new IllegalArgumentException("Invalid object identity");
        }
        try {
            MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
            for (int i = acl.getEntries().size() - 1; i >= 0; i--) {
                acl.deleteAce(i);
            }
            aclService.updateAcl(acl);
        } catch (NotFoundException ignored) {
        }
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
    public void removePermissions(String repositoryId, String path, List<Sid> sids) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        if (sids == null) {
            return;
        }
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        removePermissions(oi, sids);
    }

    private void removePermissions(ObjectIdentity objectIdentity, List<Sid> sids) {
        if (!(Objects.equals(objectIdentity.getType(),
            ProjectArtifact.class.getName()) || (Objects.equals(objectIdentity.getType(), Root.class.getName())))) {
            throw new IllegalArgumentException("Invalid object identity");
        }
        if (sids == null) {
            return;
        }
        try {
            MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
            List<Integer> indexes = new ArrayList<>();
            for (int i = acl.getEntries().size() - 1; i >= 0; i--) {
                AccessControlEntry ace = acl.getEntries().get(i);
                if (sids.contains(ace.getSid())) {
                    indexes.add(i);
                }
            }
            for (Integer index : indexes) {
                acl.deleteAce(index);
            }
            aclService.updateAcl(acl);
        } catch (NotFoundException ignored) {
        }
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
    public void removePermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        removePermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removePermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        removePermissions(oi, permissions);
    }

    private void removePermissions(ObjectIdentity objectIdentity, Map<Sid, List<Permission>> permissions) {
        if (!(Objects.equals(objectIdentity.getType(),
            ProjectArtifact.class.getName()) || (Objects.equals(objectIdentity.getType(), Root.class.getName())))) {
            throw new IllegalArgumentException("Invalid object identity");
        }
        if (permissions == null) {
            return;
        }
        try {
            MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
            List<Integer> indexes = new ArrayList<>();
            for (int i = acl.getEntries().size() - 1; i >= 0; i--) {
                AccessControlEntry ace = acl.getEntries().get(i);
                if (ace.isGranting()) {
                    List<Permission> p = permissions.get(ace.getSid());
                    if (p != null && p.contains(ace.getPermission())) {
                        indexes.add(i);
                    }
                }
            }
            for (Integer index : indexes) {
                acl.deleteAce(index);
            }
            aclService.updateAcl(acl);
        } catch (NotFoundException ignored) {
        }
    }

    @Override
    @Transactional
    public void removeRootPermissions(List<Permission> permissions, List<Sid> sids) {
        removePermissions(getRootObjectIdentity(), joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removeRootPermissions(List<Sid> sids) {
        removePermissions(getRootObjectIdentity(), sids);
    }

    @Override
    @Transactional
    public void removeRootPermissions() {
        removePermissions(getRootObjectIdentity());
    }

    private void movePermissions(ObjectIdentity oldObjectIdentity,
            Function<ObjectIdentity, ObjectIdentity> mapFunction,
            boolean deleteChildren) {
        ObjectIdentity newObjectIdentity = mapFunction.apply(oldObjectIdentity);
        MutableAcl oldAcl = getOrCreateAcl(oldObjectIdentity);
        MutableAcl newParentAcl = getOrCreateAcl(buildParentObjectIdentity(newObjectIdentity));
        MutableAcl newAcl = aclService.createAcl(newObjectIdentity);
        newAcl.setParent(newParentAcl);
        newAcl.setEntriesInheriting(true);
        for (AccessControlEntry accessControlEntry : oldAcl.getEntries()) {
            newAcl.insertAce(newAcl.getEntries().size(),
                accessControlEntry.getPermission(),
                accessControlEntry.getSid(),
                accessControlEntry.isGranting());
        }
        newAcl.setOwner(oldAcl.getOwner());
        aclService.updateAcl(newAcl);
        List<ObjectIdentity> children = aclService.findChildren(oldObjectIdentity);
        if (children != null) {
            for (ObjectIdentity child : children) {
                movePermissions(child, mapFunction, false);
            }
        }
        if (deleteChildren) {
            aclService.deleteAcl(oldObjectIdentity, true);
        }
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
    public void move(String repositoryId, String path, String newPath) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        Objects.requireNonNull(path, "path cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        moveInternal(repositoryId, oi, newPath);
    }

    private void moveInternal(String repositoryId, ObjectIdentity oi, String newPath) {
        Function<ObjectIdentity, ObjectIdentity> mapFunction = e1 -> {
            String s = ((String) e1.getIdentifier());
            s = concat(repositoryId, newPath) + s.substring(((String) oi.getIdentifier()).length());
            return new ObjectIdentityImpl(ProjectArtifact.class, s);
        };
        movePermissions(oi, mapFunction, true);
    }

    private boolean isGranted(ObjectIdentity objectIdentity, List<Sid> sids, List<Permission> permissions) {
        if (permissions == null || sids == null) {
            return false;
        }
        if (sids.contains(relevantSystemWideSid)) {
            return true;
        }
        try {
            MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
            try {
                return acl.isGranted(permissions, sids, false);
            } catch (NotFoundException nfe) {
                return false;
            }
        } catch (NotFoundException nfe) {
            ObjectIdentity poi = buildParentObjectIdentity(objectIdentity);
            return poi != null && isGranted(poi, sids, permissions);
        }
    }

    @Override
    @Transactional
    public boolean isGranted(String repositoryId, String path, List<Permission> permissions) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        if (LocalWorkspace.LOCAL_ID.equals(repositoryId)) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids = getSidRetrievalStrategy().getSids(authentication);
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        return isGranted(oi, sids, permissions);
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
    public void deleteAcl(String repositoryId, String path) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        aclService.deleteAcl(oi, true);
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
    public void deleteAclRoot() {
        aclService.deleteAcl(getRootObjectIdentity(), true);
    }

    @Override
    @Transactional
    public boolean createAcl(String repositoryId, String path, List<Permission> permissions) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, concat(repositoryId, path));
        return createAcl(oi, permissions);
    }

    private boolean createAcl(ObjectIdentity oi, List<Permission> permissions) {
        try {
            aclService.readAclById(oi);
            return false;
        } catch (NotFoundException e) {
            ObjectIdentity poi = buildParentObjectIdentity(oi);
            MutableAcl pacl = getOrCreateAcl(poi);
            MutableAcl acl = aclService.createAcl(oi);
            acl.setParent(pacl);
            acl.setEntriesInheriting(true);
            int i = 0;
            Sid sid = new PrincipalSid(SecurityContextHolder.getContext().getAuthentication());
            for (Permission permission : permissions) {
                acl.insertAce(i, permission, sid, true);
                i++;
            }
            aclService.updateAcl(acl);
            return true;
        }
    }

    @Override
    @Transactional
    public boolean createAcl(AProjectArtefact artefact, List<Permission> permissions) {
        ObjectIdentity oi = buildObjectIdentity(artefact);
        return createAcl(oi, permissions);
    }

    @Override
    @Transactional
    public void deleteSid(Sid sid) {
        aclService.deleteSid(sid, relevantSystemWideSid);
    }
}
