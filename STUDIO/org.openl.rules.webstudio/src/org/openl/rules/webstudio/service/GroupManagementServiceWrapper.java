package org.openl.rules.webstudio.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openl.rules.security.Group;

public class GroupManagementServiceWrapper extends GroupManagementService {
    private GroupManagementService delegate;

    public GroupManagementServiceWrapper() {
        super(null);
    }

    public void setDelegate(GroupManagementService delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Group> getGroups() {
        if (delegate == null) {
            return Collections.emptyList();
        }
        return delegate.getGroups();
    }

    @Override
    public Group getGroupByName(String name) {
        if (delegate == null) {
            return null;
        }

        return delegate.getGroupByName(name);
    }

    @Override
    public boolean isGroupExist(String name) {
        return delegate != null && delegate.isGroupExist(name);
    }

    @Override
    public void addGroup(String name, String description) {
        if (delegate == null) {
            return;
        }
        delegate.addGroup(name, description);
    }

    @Override
    public void updateGroup(String name, String newName, String description) {
        if (delegate == null) {
            return;
        }
        delegate.updateGroup(name, newName, description);
    }

    @Override
    public void updateGroup(String name, Set<String> groups, Set<String> privileges) {
        if (delegate == null) {
            return;
        }
        delegate.updateGroup(name, groups, privileges);
    }

    @Override
    public void deleteGroup(String name) {
        if (delegate == null) {
            return;
        }
        delegate.deleteGroup(name);
    }
}
