package org.openl.security.acl.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
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

import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.security.acl.MutableAclService;
import org.openl.util.StringUtils;

public class SimpleRepositoryAclServiceImpl implements SimpleRepositoryAclService {
    protected final MutableAclService aclService;
    private Sid relevantSystemWideSid;
    private final String rootId;
    private final Class<?> objectIdentityClass;
    private final SpringCacheBasedAclCache springCacheBasedAclCache;

    private final static int MAX_LIFE_TIME = 15000;
    private final Map<ObjectIdentity, Long> objectIdentityIdCache = new ConcurrentHashMap<>();

    private SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

    public SimpleRepositoryAclServiceImpl(SpringCacheBasedAclCache springCacheBasedAclCache,
                                          MutableAclService aclService,
                                          String rootId,
                                          Class<?> objectIdentityClass) {
        this.springCacheBasedAclCache = springCacheBasedAclCache;
        this.aclService = aclService;
        this.rootId = rootId;
        this.objectIdentityClass = objectIdentityClass;
    }

    public SimpleRepositoryAclServiceImpl(SpringCacheBasedAclCache springCacheBasedAclCache,
                                          MutableAclService aclService,
                                          String rootId,
                                          Class<?> objectIdentityClass,
                                          Sid relevantSystemWideSid) {
        this(springCacheBasedAclCache, aclService, rootId, objectIdentityClass);
        this.relevantSystemWideSid = relevantSystemWideSid;
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

    protected void evictCache(ObjectIdentity objectIdentity) {
        springCacheBasedAclCache.evictFromCache(objectIdentity);
    }

    public SidRetrievalStrategy getSidRetrievalStrategy() {
        return sidRetrievalStrategy;
    }

    public void setSidRetrievalStrategy(SidRetrievalStrategy sidRetrievalStrategy) {
        this.sidRetrievalStrategy = sidRetrievalStrategy;
    }

    public static ObjectIdentity buildParentObjectIdentity(ObjectIdentity oi,
                                                           Class<?> objectIdentityClass,
                                                           String rootId) {
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
            return new ObjectIdentityImpl(objectIdentityClass, parentObjectIdentity);
        }
        return getRootObjectIdentity(rootId);
    }

    protected ObjectIdentity buildParentObjectIdentity(ObjectIdentity oi) {
        return buildParentObjectIdentity(oi, getObjectIdentityClass(), getRootId());
    }

    public String getRootId() {
        return rootId;
    }

    public Class<?> getObjectIdentityClass() {
        return objectIdentityClass;
    }

    private ObjectIdentity getRootObjectIdentity() {
        return getRootObjectIdentity(getRootId());
    }

    private static ObjectIdentity getRootObjectIdentity(String rootId) {
        return new ObjectIdentityImpl(Root.class, rootId);
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
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        return listPermissions(oi, null);
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listPermissions(String repositoryId, String path, List<Sid> sids) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        return listPermissions(oi, sids);
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listRootPermissions() {
        return listPermissions(getRootObjectIdentity(), null);
    }

    @Override
    @Transactional
    public Map<Sid, List<Permission>> listRootPermissions(List<Sid> sids) {
        return listPermissions(getRootObjectIdentity(), sids);
    }

    @Override
    @Transactional
    public void addPermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
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
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        addPermissions(oi, joinSidsAndPermissions(permissions, sids));
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
    public void removePermissions(String repositoryId, String path) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
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
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        if (sids == null) {
            return;
        }
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        removePermissions(oi, sids);
    }

    protected void removePermissions(ObjectIdentity objectIdentity, List<Sid> sids) {
        if (!(Objects.equals(objectIdentity.getType(),
                getObjectIdentityClass().getName()) || (Objects.equals(objectIdentity.getType(), Root.class.getName())))) {
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
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        removePermissions(oi, joinSidsAndPermissions(permissions, sids));
    }

    @Override
    @Transactional
    public void removePermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        removePermissions(oi, permissions);
    }

    protected void removePermissions(ObjectIdentity objectIdentity, Map<Sid, List<Permission>> permissions) {
        if (!(Objects.equals(objectIdentity.getType(),
                getObjectIdentityClass().getName()) || (Objects.equals(objectIdentity.getType(), Root.class.getName())))) {
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

    protected void movePermissions(ObjectIdentity oldObjectIdentity,
                                   Function<ObjectIdentity, ObjectIdentity> mapFunction,
                                   boolean deleteChildren) {
        ObjectIdentity newObjectIdentity = mapFunction.apply(oldObjectIdentity);
        MutableAcl oldAcl = getOrCreateAcl(oldObjectIdentity);
        MutableAcl newParentAcl = getOrCreateAcl(buildParentObjectIdentity(newObjectIdentity));
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
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        Objects.requireNonNull(path, "path cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        moveInternal(repositoryId, oi, newPath);
    }

    protected void moveInternal(String repositoryId, ObjectIdentity oi, String newPath) {
        Function<ObjectIdentity, ObjectIdentity> mapFunction = e1 -> {
            String s = ((String) e1.getIdentifier());
            s = concat(repositoryId, newPath) + s.substring(((String) oi.getIdentifier()).length());
            return new ObjectIdentityImpl(getObjectIdentityClass(), s);
        };
        movePermissions(oi, mapFunction, true);
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
                ObjectIdentity poi = buildParentObjectIdentity(objectIdentity);
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
            ObjectIdentity poi = buildParentObjectIdentity(objectIdentity);
            return poi != null && isGranted(poi, sids, permissions);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGranted(String repositoryId, String path, List<Permission> permissions) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        if (LocalWorkspace.LOCAL_ID.equals(repositoryId)) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Sid> sids = getSidRetrievalStrategy().getSids(authentication);
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        return isGranted(oi, sids, permissions);
    }

    @Override
    @Transactional
    public void deleteAcl(String repositoryId, String path) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        aclService.deleteAcl(oi, true);
        objectIdentityIdCache.remove(oi);
    }

    @Override
    @Transactional
    public void deleteAclRoot() {
        ObjectIdentity oi = getRootObjectIdentity();
        aclService.deleteAcl(oi, true);
    }

    @Override
    @Transactional
    public boolean createAcl(String repositoryId, String path, List<Permission> permissions, boolean force) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        return createAcl(oi, permissions, force);
    }

    @Override
    @Transactional
    public boolean hasAcl(String repositoryId, String path) {
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
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
            ObjectIdentity poi = buildParentObjectIdentity(oi);
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
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
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
        Objects.requireNonNull(repositoryId, "repositoryId cannot be null");
        ObjectIdentity oi = new ObjectIdentityImpl(getObjectIdentityClass(), concat(repositoryId, path));
        return getOwner(oi);
    }
}
