package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.security.acl.JdbcSidManagingAclService;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditChanges;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.common.exception.NotFoundException;

/**
 * Keeps the groups of OpenL Studio and their privileges.
 *
 * <p>Every change of a group is announced as an {@link AuditEvent}, which the audit log records in the transaction of
 * the change: a group created, renamed or described anew, its privileges changed, the group deleted. A change that
 * leaves a value as it was announces nothing.
 *
 * @author Andrei Astrouski
 */
@RequiredArgsConstructor
public class GroupManagementService {

    private final GroupDao groupDao;
    private final JdbcSidManagingAclService aclService;
    private final ApplicationEventPublisher eventPublisher;

    public List<org.openl.rules.security.Group> getGroups() {
        List<Group> groups = groupDao.getAllGroups();
        return groups.stream().map(PrivilegesEvaluator::wrap).collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    public List<String> findGroupNames(String searchTerm, int limit) {
        return groupDao.findGroupNames(searchTerm, limit);
    }

    public org.openl.rules.security.Group getGroupByName(String name) {
        var group = groupDao.getGroupByName(name);
        if (group != null) {
            return PrivilegesEvaluator.wrap(group);
        }
        return null;
    }

    @Transactional
    public void addGroup(String name, String description) {
        var persistGroup = new Group();
        persistGroup.setName(name);
        persistGroup.setDescription(description);
        groupDao.save(persistGroup);
        announce(AuditAction.GROUP_CREATED, name, Map.of());
    }

    @Transactional
    public void updateGroup(String name, String newName, String description) {
        var persistGroup = groupOf(name);
        var changes = new AuditChanges()
                .compare("name", name, newName)
                .compare("description", persistGroup.getDescription(), description);
        persistGroup.setName(newName);
        persistGroup.setDescription(description);
        groupDao.update(persistGroup);
        aclService.updateSid(new GrantedAuthoritySid(name), newName);
        if (!changes.isEmpty()) {
            announce(AuditAction.GROUP_UPDATED, newName, Map.of("changed", changes.asMap()));
        }
    }

    /**
     * Sets the privileges of a group, and announces the privileges it gained and lost.
     */
    @Transactional
    public void updateGroup(String name, Set<String> privileges) {
        var persistGroup = groupOf(name);
        // A group created in the same session has no privileges read yet.
        var before = Optional.ofNullable(persistGroup.getPrivileges()).map(Set::copyOf).orElseGet(Set::of);
        persistGroup.setPrivileges(new HashSet<>(privileges));
        groupDao.update(persistGroup);
        var changes = AuditChanges.addedAndRemoved(before, privileges);
        if (!changes.isEmpty()) {
            announce(AuditAction.GROUP_PRIVILEGES_CHANGED, name, changes);
        }
    }

    @Transactional
    public void deleteGroup(Long id) {
        var persistGroup = groupDao.getGroupById(id);
        groupDao.deleteGroupById(id);
        if (persistGroup != null) {
            announce(AuditAction.GROUP_DELETED, persistGroup.getName(), Map.of());
        }
    }

    /**
     * The group of a name, which must exist.
     *
     * @throws NotFoundException when no group has the name
     */
    private Group groupOf(String name) {
        return Optional.ofNullable(groupDao.getGroupByName(name))
                .orElseThrow(() -> new NotFoundException("group.message"));
    }

    private void announce(AuditAction action, String group, Map<String, ?> details) {
        eventPublisher.publishEvent(AuditEvent.builder()
                .action(action)
                .targetType(AuditTargetType.GROUP)
                .target(group)
                .details(details)
                .build());
    }

    @Transactional
    public boolean existsByName(String name) {
        return groupDao.existsByName(name);
    }

    @Transactional(readOnly = true)
    public long countUsersInGroup(String groupName) {
        return groupDao.countUsersInGroup(groupName);
    }

    @Transactional(readOnly = true)
    public Set<String> getGroupNames() {
        return groupDao.getGroupNames();
    }
}
