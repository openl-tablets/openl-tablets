package org.openl.security.acl.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.security.acl.MutableAclService;
import org.openl.security.acl.oid.AclObjectIdentityProvider;

public class SimpleRepositoryAclServiceImpl implements SimpleRepositoryAclService {

    protected final MutableAclService aclService;
    private final Sid relevantSystemWideSid;
    private final AclCache springCacheBasedAclCache;
    protected final SidRetrievalStrategy sidRetrievalStrategy;

    private final static int MAX_LIFE_TIME = 15000;
    private final Map<ObjectIdentity, Long> objectIdentityIdCache = new ConcurrentHashMap<>();

    protected final AclObjectIdentityProvider oidProvider;

    public SimpleRepositoryAclServiceImpl(AclCache springCacheBasedAclCache,
                                          MutableAclService aclService,
                                          Sid relevantSystemWideSid,
                                          SidRetrievalStrategy sidRetrievalStrategy,
                                          AclObjectIdentityProvider oidProvider) {
        this.springCacheBasedAclCache = springCacheBasedAclCache;
        this.aclService = aclService;
        this.relevantSystemWideSid = relevantSystemWideSid;
        this.sidRetrievalStrategy = sidRetrievalStrategy;
        this.oidProvider = oidProvider;
    }

    protected void evictCache(ObjectIdentity objectIdentity) {
        springCacheBasedAclCache.evictFromCache(objectIdentity);
    }

    protected MutableAcl getOrCreateAcl(ObjectIdentity oi) {
        MutableAcl acl;
        try {
            evictCache(oi);
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
            objectIdentityIdCache.remove(oi);
            acl.setEntriesInheriting(true);
            ObjectIdentity poi = oidProvider.getParentOid(oi);
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
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        return listPermissions(oi, null);
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listPermissions(String repositoryId, String path, List<Sid> sids) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        return listPermissions(oi, sids);
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listRootPermissions() {
        var rootOid = oidProvider.getRootOid();
        return listPermissions(rootOid, null);
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listRootPermissions(List<Sid> sids) {
        var rootOid = oidProvider.getRootOid();
        return listPermissions(rootOid, sids);
    }

    @Override
    @Transactional
    public void addPermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        addPermissions(oi, permissions);
    }

    protected Map<Sid, List<Permission>> listPermissions(ObjectIdentity objectIdentity, List<Sid> sids) {
        try {
            evictCache(objectIdentity);
            Map<Sid, List<Permission>> map = new HashMap<>();
            Acl acl = sids == null ? aclService.readAclById(objectIdentity)
                    : aclService.readAclById(objectIdentity, sids);
            for (AccessControlEntry ace : acl.getEntries()) {
                if (ace.isGranting()) {
                    if (sids == null || sids.contains(ace.getSid())) {
                        List<Permission> p = map.computeIfAbsent(ace.getSid(), k -> new ArrayList<>());
                        p.add(ace.getPermission());
                    }
                }
            }
            return map;
        } catch (NotFoundException e) {
            return Collections.emptyMap();
        }
    }

    protected void addPermissions(ObjectIdentity objectIdentity, Map<Sid, List<Permission>> permissions) {
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

    protected Map<Sid, List<Permission>> joinSidsAndPermissions(List<Permission> permissions, List<Sid> sids) {
        Map<Sid, List<Permission>> ret = new HashMap<>();
        for (Sid sid : sids) {
            ret.put(sid, permissions);
        }
        return ret;
    }

    @Override
    @Transactional
    public void addPermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        addPermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void addRootPermissions(Map<Sid, List<Permission>> permissions) {
        var rootOid = oidProvider.getRootOid();
        addPermissions(rootOid, permissions);
    }

    @Override
    @Transactional
    public void addRootPermissions(List<Permission> permissions, List<Sid> sids) {
        var rootOid = oidProvider.getRootOid();
        addPermissions(rootOid, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removePermissions(String repositoryId, String path) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        removePermissions(oi);
    }

    protected void removePermissions(ObjectIdentity objectIdentity) {
        try {
            evictCache(objectIdentity);
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
    public void removePermissions(String repositoryId, String path, List<Sid> sids) {
        if (sids == null) {
            return;
        }
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        removePermissions(oi, sids);
    }

    protected void removePermissions(ObjectIdentity objectIdentity, List<Sid> sids) {
        if (!(Objects.equals(objectIdentity.getType(), oidProvider.getOidType())
                || (Objects.equals(objectIdentity.getType(), Root.class.getName())))) {
            throw new IllegalArgumentException("Invalid object identity");
        }
        if (sids == null) {
            return;
        }
        try {
            evictCache(objectIdentity);
            MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
            for (int i = acl.getEntries().size() - 1; i >= 0; i--) {
                AccessControlEntry ace = acl.getEntries().get(i);
                if (sids.contains(ace.getSid())) {
                    acl.deleteAce(i);
                }
            }
            aclService.updateAcl(acl);
        } catch (NotFoundException ignored) {
        }
    }

    @Override
    @Transactional
    public void removePermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        removePermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removePermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        removePermissions(oi, permissions);
    }

    protected void removePermissions(ObjectIdentity objectIdentity, Map<Sid, List<Permission>> permissions) {
        if (!(Objects.equals(objectIdentity.getType(), oidProvider.getOidType())
                || (Objects.equals(objectIdentity.getType(), Root.class.getName())))) {
            throw new IllegalArgumentException("Invalid object identity");
        }
        if (permissions == null) {
            return;
        }
        try {
            evictCache(objectIdentity);
            MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
            for (int i = acl.getEntries().size() - 1; i >= 0; i--) {
                AccessControlEntry ace = acl.getEntries().get(i);
                if (ace.isGranting()) {
                    List<Permission> p = permissions.get(ace.getSid());
                    if (p != null && p.contains(ace.getPermission())) {
                        acl.deleteAce(i);
                    }
                }
            }
            aclService.updateAcl(acl);
        } catch (NotFoundException ignored) {
        }
    }

    @Override
    @Transactional
    public void removeRootPermissions(List<Permission> permissions, List<Sid> sids) {
        var rootOid = oidProvider.getRootOid();
        removePermissions(rootOid, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removeRootPermissions(List<Sid> sids) {
        var rootOid = oidProvider.getRootOid();
        removePermissions(rootOid, sids);
    }

    @Override
    @Transactional
    public void removeRootPermissions() {
        var rootOid = oidProvider.getRootOid();
        removePermissions(rootOid);
    }

    protected void movePermissions(ObjectIdentity oldObjectIdentity,
                                   Function<ObjectIdentity, ObjectIdentity> mapFunction,
                                   boolean deleteChildren) {
        ObjectIdentity newObjectIdentity = mapFunction.apply(oldObjectIdentity);
        MutableAcl oldAcl = getOrCreateAcl(oldObjectIdentity);
        MutableAcl newParentAcl = getOrCreateAcl(oidProvider.getParentOid(newObjectIdentity));
        MutableAcl newAcl = aclService.createAcl(newObjectIdentity);
        objectIdentityIdCache.remove(newObjectIdentity);
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
    public void move(String repositoryId, String path, String newPath) {
        Objects.requireNonNull(path, "path cannot be null");
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        moveInternal(repositoryId, oi, newPath);
    }

    protected void moveInternal(String repositoryId, ObjectIdentity oldParentOid, String newPath) {
        var newParentOid = oidProvider.getRepositoryOid(repositoryId, newPath);
        Function<ObjectIdentity, ObjectIdentity> mapFunction = childOid -> oidProvider.moveToNewParent(childOid, oldParentOid, newParentOid);
        movePermissions(oldParentOid, mapFunction, true);
    }

    protected boolean isGranted(ObjectIdentity objectIdentity, List<Sid> sids, List<Permission> permissions) {
        if (permissions == null || sids == null) {
            return false;
        }
        if (sids.contains(relevantSystemWideSid)) {
            return true;
        }
        Long t = objectIdentityIdCache.get(objectIdentity);
        if (t != null) {
            // This is a performance optimization to avoid hitting the database
            if (System.currentTimeMillis() - t <= MAX_LIFE_TIME) {
                ObjectIdentity poi = oidProvider.getParentOid(objectIdentity);
                return poi != null && isGranted(poi, sids, permissions);
            }
        }
        try {
            MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
            try {
                return acl.isGranted(permissions, sids, false);
            } catch (NotFoundException nfe) {
                return false;
            }
        } catch (NotFoundException nfe) {
            objectIdentityIdCache.put(objectIdentity, System.currentTimeMillis());
            ObjectIdentity poi = oidProvider.getParentOid(objectIdentity);
            return poi != null && isGranted(poi, sids, permissions);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGranted(String repositoryId, String path, List<Permission> permissions) {
        if (LocalWorkspace.LOCAL_ID.equals(repositoryId)) {
            return true;
        }
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids = sidRetrievalStrategy.getSids(authentication);
        return isGranted(oi, sids, permissions);
    }

    @Override
    @Transactional
    public void deleteAcl(String repositoryId, String path) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        aclService.deleteAcl(oi, true);
        objectIdentityIdCache.remove(oi);
    }

    @Override
    @Transactional
    public void deleteAclRoot() {
        var rootOid = oidProvider.getRootOid();
        aclService.deleteAcl(rootOid, true);
    }

    @Override
    @Transactional
    public boolean createAcl(String repositoryId, String path, List<Permission> permissions, boolean force) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        return createAcl(oi, permissions, force);
    }

    @Override
    @Transactional
    public boolean hasAcl(String repositoryId, String path) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        return hasAcl(oi);
    }

    protected boolean hasAcl(ObjectIdentity oi) {
        try {
            aclService.readAclById(oi);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    protected boolean tryCreateAcl(ObjectIdentity oi, List<Permission> permissions) {
        try {
            aclService.readAclById(oi);
            return false;
        } catch (NotFoundException e) {
            ObjectIdentity poi = oidProvider.getParentOid(oi);
            MutableAcl pacl = getOrCreateAcl(poi);
            MutableAcl acl = aclService.createAcl(oi);
            objectIdentityIdCache.remove(oi);
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

    protected boolean createAcl(ObjectIdentity oi, List<Permission> permissions, boolean force) {
        boolean created = tryCreateAcl(oi, permissions);
        if (!created && force) {
            aclService.deleteAcl(oi, true);
            return tryCreateAcl(oi, permissions);
        }
        return created;
    }

    protected boolean updateOwner(ObjectIdentity oi, Sid newOwner) {
        try {
            MutableAcl acl = (MutableAcl) aclService.readAclById(oi);
            acl.setOwner(newOwner);
            aclService.updateAcl(acl);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateOwner(String repositoryId, String path, Sid newOwner) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        return updateOwner(oi, newOwner);
    }

    protected Sid getOwner(ObjectIdentity oi) {
        try {
            MutableAcl acl = getOrCreateAcl(oi);
            return acl.getOwner();
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    @Transactional
    public Sid getOwner(String repositoryId, String path) {
        ObjectIdentity oi = oidProvider.getRepositoryOid(repositoryId, path);
        return getOwner(oi);
    }
}
