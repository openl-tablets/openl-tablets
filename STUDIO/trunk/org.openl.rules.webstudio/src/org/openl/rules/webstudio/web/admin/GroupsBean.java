package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.security.DefaultPrivileges;
import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.service.GroupManagementService;

/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class GroupsBean {

    @NotBlank(message="Can not be empty")
    @Size(max=25)
    private String name;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ManagedProperty(value="#{groupManagementService}")
    private GroupManagementService groupManagementService;

    /**
     * Validation for existed group
     */
    public void validateGroupName(FacesContext context, UIComponent toValidate, Object value) {
        if (groupManagementService.isGroupExist((String) value)) {
            throw new ValidatorException(
                    new FacesMessage("Group with such name already exists"));
        }
    }

    public Privilege[] getDefaultPrivileges() {
        return DefaultPrivileges.values();
    }

    public List<String> getPrivileges(String groupName) {
        List<String> result = new ArrayList<String>();
        Group group = groupManagementService.getGroupByName(groupName);
        Collection<Privilege> privileges = group.getPrivileges();
        for (Privilege privilege : privileges) {
            if (privilege instanceof Group) {
                result.addAll(getPrivileges(privilege.getName()));
            } else {
                result.add(privilege.getName());
            }
        }
        return result;
    }

    public List<Group> getGroups() {
        return groupManagementService.getGroups();
    }

    public void addGroup() {
        Collection<Privilege> authorities = new ArrayList<Privilege>();

        List<String> privileges = new ArrayList<String>(Arrays.asList(
                FacesUtils.getRequest().getParameterValues("privilege")));

        Map<String, Group> groups = new java.util.HashMap<String, Group>();
        String[] groupNames = FacesUtils.getRequest().getParameterValues("group");
        if (groupNames != null) {
            for (String groupName : groupNames) {
                groups.put(groupName, groupManagementService.getGroupByName(groupName));
            }

            for (Group group : new ArrayList<Group>(groups.values())) {
                if (!groups.isEmpty()) {
                    removeIncludedGroups(group, groups);
                }
            }

            removeIncludedPrivileges(privileges, groups);

            for (Group group : groups.values()) {
                authorities.add(group);
            }
        }

        for (String privilegeName : privileges) {
            authorities.add(DefaultPrivileges.valueOf(privilegeName));
        }

        groupManagementService.addGroup(new SimpleGroup(name, description, authorities));
    }

    private void removeIncludedGroups(Group group, Map<String, Group> groups) {
        Set<String> groupNames = new HashSet<String>(groups.keySet());
        for (String checkGroupName : groupNames) {
            if (!group.getName().equals(checkGroupName) &&
                    group.hasPrivilege(checkGroupName)) {
                Group includedGroup = groups.get(checkGroupName);
                if (includedGroup != null) {
                    removeIncludedGroups(includedGroup, groups);
                    groups.remove(checkGroupName);
                }
            }
        }
    }

    private void removeIncludedPrivileges(List<String> privileges, Map<String, Group> groups) {
        for (String privilege : new ArrayList<String>(privileges)) {
            for (Group group : groups.values()) {
                if (group.hasPrivilege(privilege)) {
                    privileges.remove(privilege);
                }
            }
        }
    }

    public boolean isOnlyAdmin(Object objGroup) {
        String allPrivileges = DefaultPrivileges.PRIVILEGE_ALL.name();
        return ((Group) objGroup).hasPrivilege(allPrivileges)
                && groupManagementService.getGroupsByPrivilege(allPrivileges).size() == 1;
    }

    public void deleteGroup(String name) {
        groupManagementService.deleteGroup(name);
    }

    public void setGroupManagementService(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

}
