package org.openl.rules.webstudio.service;

import java.util.Collections;
import java.util.List;

import org.openl.rules.security.Group;

public class GroupManagementServiceWrapper implements GroupManagementService {
    private GroupManagementService delegate;

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
    public void addGroup(Group group) {
        if (delegate == null) {
            return;
        }
        delegate.addGroup(group);
    }

    @Override
    public void updateGroup(String name, Group group) {
        if (delegate == null) {
            return;
        }
        delegate.updateGroup(name, group);
    }

    @Override
    public void deleteGroup(String name) {
        if (delegate == null) {
            return;
        }
        delegate.deleteGroup(name);
    }
}
