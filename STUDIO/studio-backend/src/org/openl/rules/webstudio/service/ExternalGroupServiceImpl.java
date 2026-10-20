package org.openl.rules.webstudio.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.openl.rules.security.Group;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.dao.ExternalGroupDao;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.ExternalGroup;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditChanges;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.audit.service.ExternalGroupRolesAuditor;

/**
 * External groups service implementation
 *
 * <p>The identity provider reports the groups of a user at every sign-in. A change of them is announced to the audit
 * log with what it brings: the groups of OpenL Studio of the same names the user joined or left, and the roles those
 * groups hold that the user gained or lost.
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
@Slf4j
public class ExternalGroupServiceImpl implements ExternalGroupService {


    private final ExternalGroupDao externalGroupDao;
    private final GroupDao groupDao;
    private final LockRegistry lockRegistry;
    private final TransactionTemplate txTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<ExternalGroupRolesAuditor> rolesAuditor;

    @Override
    @Transactional
    public void deleteAll() {
        externalGroupDao.deleteAll();
    }

    @Override
    public void mergeAllForUser(String loginName, Collection<? extends GrantedAuthority> externalGroups) {
        var change = mergeGroups(loginName, externalGroups);
        if (change != null) {
            announceWhatTheGroupsBring(loginName, change);
        }
    }

    /**
     * Saves the external groups the identity provider reports for a user when they differ from the saved ones, and
     * tells how they changed. The groups of one user are merged by one sign-in at a time.
     */
    private @Nullable GroupChange mergeGroups(String loginName, Collection<? extends GrantedAuthority> externalGroups) {
        var lock = lockRegistry.obtain("externalGroupMergeLock_" + loginName);
        var lockAcquired = false;
        try {
            lockAcquired = lock.tryLock(30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new DataAccessResourceFailureException("Cannot acquire lock for user: " + loginName);
            }
            // transaction must be started after lock is acquired
            return txTemplate.execute(status -> {
                var before = externalGroupDao.findGroupNamesForUser(loginName);
                var after = externalGroups.stream().map(GrantedAuthority::getAuthority).toList();
                var change = new GroupChange(AuditChanges.added(before, after), AuditChanges.removed(before, after));
                // The groups are written again only when the identity provider reports others.
                if (change.isEmpty()) {
                    return null;
                }
                externalGroupDao.deleteAllForUser(loginName);
                externalGroupDao.save(new BatchCreateExternalGroupCursor(loginName, externalGroups));
                announceChange(loginName, change);
                return change;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Thread interrupted", e);
            return null;
        } finally {
            if (lockAcquired) {
                lock.unlock();
            }
        }
    }

    /**
     * Announces the groups the identity provider added to the user and took away.
     */
    private void announceChange(String loginName, GroupChange change) {
        eventPublisher.publishEvent(AuditEvent.builder()
                .action(AuditAction.EXTERNAL_GROUPS_CHANGED)
                .targetType(AuditTargetType.USER)
                .target(loginName)
                .details(Map.of("added", change.joined(), "removed", change.left()))
                .build());
    }

    /**
     * The external groups the identity provider gave a user and took away.
     */
    private record GroupChange(Set<String> joined, Set<String> left) {

        boolean isEmpty() {
            return joined.isEmpty() && left.isEmpty();
        }
    }

    /**
     * Announces the groups of OpenL Studio and the roles the user gained and lost with its external groups.
     *
     * <p>Working them out reads the groups and the access control lists, so it runs once the external groups are
     * saved, in a transaction of its own. A failure is logged and leaves the groups the identity provider reported
     * saved: it never fails the sign-in.
     */
    private void announceWhatTheGroupsBring(String loginName, GroupChange change) {
        try {
            txTemplate.executeWithoutResult(status -> {
                announceMemberships(loginName, change.joined(), change.left());
                rolesAuditor.ifAvailable(auditor -> auditor.announce(loginName, change.joined(), change.left()));
            });
        } catch (RuntimeException e) {
            log.warn("Failed to announce the groups and roles user '{}' gained or lost at sign-in.", loginName, e);
        }
    }

    /**
     * Announces the groups of OpenL Studio the user joined or left with its external groups of the same names, each
     * with the privileges it gives. An external group that names no group of OpenL Studio gives no privileges.
     */
    private void announceMemberships(String loginName, Set<String> joined, Set<String> left) {
        var privileges = new HashMap<String, Set<String>>();
        groupDao.getGroupsByNames(Stream.concat(joined.stream(), left.stream()).toList())
                .forEach(group -> privileges.put(group.getName(), new TreeSet<>(group.getPrivileges())));
        joined.stream().filter(privileges::containsKey).forEach(group -> announceMembership(
                AuditAction.GROUP_MEMBER_ADDED, loginName, group, privileges.get(group)));
        left.stream().filter(privileges::containsKey).forEach(group -> announceMembership(
                AuditAction.GROUP_MEMBER_REMOVED, loginName, group, privileges.get(group)));
    }

    private void announceMembership(AuditAction action, String loginName, String group, Set<String> privileges) {
        var details = new LinkedHashMap<String, Object>();
        details.put(AuditEvent.ORIGIN, AuditEvent.IDENTITY_PROVIDER);
        details.put("privileges", privileges.isEmpty() ? null : privileges);
        eventPublisher.publishEvent(AuditEvent.membership(action, group, loginName).details(details).build());
    }

    @Override
    @Transactional
    public List<Group> findAllForUser(String loginName) {
        return externalGroupDao.findAllForUser(loginName)
                .stream()
                .map(ext -> new SimpleGroup(ext.getGroupName(), Set.of()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public long countAllForUser(String loginName) {
        return externalGroupDao.countAllForUser(loginName);
    }

    @Override
    @Transactional
    public List<Group> findMatchedForUser(String loginName) {
        return externalGroupDao.findMatchedForUser(loginName)
                .stream()
                .map(PrivilegesEvaluator::wrap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public long countMatchedForUser(String loginName) {
        return externalGroupDao.countMatchedForUser(loginName);
    }

    @Override
    @Transactional
    public List<Group> findNotMatchedForUser(String loginName) {
        return externalGroupDao.findNotMatchedForUser(loginName)
                .stream()
                .map(ext -> new SimpleGroup(ext.getGroupName(), Set.of()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public long countNotMatchedForUser(String loginName) {
        return externalGroupDao.countNotMatchedForUser(loginName);
    }

    @Override
    @Transactional
    public List<Group> findAllByName(String groupName, int limit) {
        return externalGroupDao.findAllByName(groupName, limit)
                .stream()
                .map(ext -> new SimpleGroup(ext, Set.of()))
                .collect(Collectors.toList());
    }

    private static class BatchCreateExternalGroupCursor implements Iterable<ExternalGroup> {

        private final String loginName;
        private final Collection<? extends GrantedAuthority> externalGroups;

        public BatchCreateExternalGroupCursor(String loginName, Collection<? extends GrantedAuthority> externalGroups) {
            this.externalGroups = externalGroups;
            this.loginName = loginName;
        }

        @Override
        public Iterator<ExternalGroup> iterator() {
            final Iterator<? extends GrantedAuthority> it = externalGroups.iterator();
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public ExternalGroup next() {
                    var group = it.next();
                    var externalGroup = new ExternalGroup();
                    externalGroup.setLoginName(loginName);
                    externalGroup.setGroupName(group.getAuthority());
                    return externalGroup;
                }
            };
        }

    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersInGroup(String groupName) {
        return externalGroupDao.countUsersInGroup(groupName);
    }
}
