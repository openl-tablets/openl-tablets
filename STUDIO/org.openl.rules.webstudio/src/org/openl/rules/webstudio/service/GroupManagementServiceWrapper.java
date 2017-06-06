package org.openl.rules.webstudio.service;

import java.util.Collections;
import java.util.List;

import org.openl.rules.security.Group;

public class GroupManagementServiceWrapper implements GroupManagementService {
    private GroupManagementService delegate;

    public void setDelegate(GroupManagementService delegate) {
        this.delegate = delegate;
    }

    public List<Group> getGroups() {
        if (delegate == null) {
            return Collections.emptyList();
        }
        return delegate.getGroups();
    }

    public Group getGroupByName(String name) {
        if (delegate == null) {
            return null;
        }

        return delegate.getGroupByName(name);
    }

    public boolean isGroupExist(String name) {
        return delegate != null && delegate.isGroupExist(name);
    }

    public void addGroup(Group group) {
        if (delegate == null) {
            return;
        }
        delegate.addGroup(group);
    }

    public void updateGroup(String name, Group group) {
        if (delegate == null) {
            return;
        }
        delegate.updateGroup(name, group);
    }

    public void deleteGroup(String name) {
        if (delegate == null) {
            return;
        }
        delegate.deleteGroup(name);
    }
}
