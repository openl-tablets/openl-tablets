package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.security.acl.JdbcMutableAclService;

/**
 * @author Andrei Astrouski
 */
public class GroupManagementService {

    private final GroupDao groupDao;
    private final JdbcMutableAclService aclService;

    public GroupManagementService(GroupDao groupDao, JdbcMutableAclService aclService) {
        this.groupDao = groupDao;
        this.aclService = aclService;
    }

    public List<org.openl.rules.security.Group> getGroups() {
        List<Group> groups = groupDao.getAllGroups();
        List<org.openl.rules.security.Group> resultGroups = new ArrayList<>();

        for (Group group : groups) {
            resultGroups.add(PrivilegesEvaluator.wrap(group));
        }

        return resultGroups;
    }

    public org.openl.rules.security.Group getGroupByName(String name) {
        Group group = groupDao.getGroupByName(name);
        if (group != null) {
            return PrivilegesEvaluator.wrap(group);
        }
        return null;
    }

    public void addGroup(String name, String description) {
        Group persistGroup = new Group();
        persistGroup.setName(name);
        persistGroup.setDescription(description);
        groupDao.save(persistGroup);
    }

    @Transactional
    public void updateGroup(String name, String newName, String description) {
        Group persistGroup = groupDao.getGroupByName(name);
        persistGroup.setName(newName);
        persistGroup.setDescription(description);
        groupDao.update(persistGroup);
        aclService.updateSid(new GrantedAuthoritySid(name), newName);
    }

    public void updateGroup(String name, Set<String> privileges) {
        Group persistGroup = groupDao.getGroupByName(name);
        persistGroup.setPrivileges(privileges);
        groupDao.update(persistGroup);
    }

    @Transactional
    public void deleteGroup(Long id) {
        groupDao.deleteGroupById(id);
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
