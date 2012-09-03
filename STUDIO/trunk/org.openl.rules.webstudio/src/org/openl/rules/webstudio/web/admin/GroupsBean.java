package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.rules.security.Group;
import org.openl.rules.webstudio.service.GroupManagementService;

/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class GroupsBean {

    @ManagedProperty(value="#{groupManagementService}")
    private GroupManagementService groupManagementService;

    public List<Group> getPredefinedGroups() {
        return groupManagementService.getPredefinedGroups();
    }

    public List<Group> getGroups() {
        return groupManagementService.getGroups();
    }

    public void deleteGroup(String name) {
        groupManagementService.deleteGroup(name);
    }

    public void setGroupManagementService(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

}
