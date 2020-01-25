package org.openl.rules.webstudio.web.admin;

import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.servlet.ServletRequest;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

// TODO Needs performance optimization
/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class GroupsBean {

    public static final String VALIDATION_EMPTY = "Cannot be empty";
    public static final String VALIDATION_MAX = "Must be less than ";

    @NotBlank(message = VALIDATION_EMPTY)
    @Size(max = 40, message = VALIDATION_MAX + 40)
    private String name;

    /* Used for editing */
    @NotBlank(message = VALIDATION_EMPTY)
    @Size(max = 40, message = VALIDATION_MAX + 40)
    private String newName;
    private String oldName;

    @Size(max = 200, message = VALIDATION_MAX + 200)
    private String description;
    private List<Group> groups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ManagedProperty(value = "#{groupManagementService}")
    private GroupManagementService groupManagementService;

    /**
     * Validation for existed group
     */
    public void validateGroupName(FacesContext context, UIComponent toValidate, Object value) {
        if (groupManagementService.isGroupExist((String) value)) {
            throw new ValidatorException(new FacesMessage("Group with such name already exists"));
        }
    }

    public Privilege[] getDefaultPrivileges() {
        return Privileges.values();
    }

    public List<String> getPrivileges(Group group) {
        List<String> result = new ArrayList<>();
        if (group == null) {
            return result;
        }
        Collection<Privilege> privileges = group.getPrivileges();
        for (Privilege privilege : privileges) {
            if (privilege instanceof Group) {
                result.addAll(getPrivileges((Group) privilege));
            } else {
                result.add(privilege.getName());
            }
        }
        return result;
    }

    public List<String> getIncludedGroups(Group group) {
        List<String> result = new ArrayList<>();
        if (group == null) {
            return result;
        }
        Collection<Privilege> authorities = group.getPrivileges();
        for (Privilege authority : authorities) {
            if (authority instanceof Group) {
                // Don't use Set
                List<String> incGroups = getIncludedGroups((Group) authority);
                for (String incGroup : incGroups) {
                    if (!result.contains(incGroup)) {
                        result.add(incGroup);
                    }
                }
                result.add(authority.getName());
            }
        }
        return result;
    }

    public List<String> getNonGroupPrivileges(Group group) {
        List<String> result = new ArrayList<>();
        if (group == null) {
            return result;
        }
        Collection<Privilege> authorities = group.getPrivileges();
        for (Privilege authority : authorities) {
            if (!(authority instanceof Group)) {
                result.add(authority.getDisplayName());
            }
        }
        return result;
    }

    public List<Group> getGroups() {
        if (groups == null) {
            groups = groupManagementService.getGroups();
        }
        return groups;
    }

    private Collection<Privilege> getSelectedAuthorities() {
        Collection<Privilege> authorities = new ArrayList<>();

        String[] privilegesParam = ((ServletRequest) WebStudioUtils.getExternalContext().getRequest()).getParameterValues("privilege");
        List<String> privileges = new ArrayList<>(
            Arrays.asList(privilegesParam == null ? new String[0] : privilegesParam));
        privileges.add(0, Privileges.VIEW_PROJECTS.name());

        // Admin
        Map<String, Group> groups = new java.util.HashMap<>();
        String[] groupNames = ((ServletRequest) WebStudioUtils.getExternalContext().getRequest()).getParameterValues("group");
        if (groupNames != null) {
            for (String groupName : groupNames) {
                if (groupName.equals(oldName)) {
                    // Persisting group should not include itself
                    continue;
                }
                groups.put(groupName, groupManagementService.getGroupByName(groupName));
            }

            for (Group group : new ArrayList<>(groups.values())) {
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
            authorities.add(Privileges.valueOf(privilegeName));
        }

        return authorities;
    }

    public void addGroup() {
        groupManagementService.addGroup(new SimpleGroup(name, description, getSelectedAuthorities()));
        groups = null;
    }

    public void editGroup() {
        groupManagementService.updateGroup(oldName, new SimpleGroup(newName, description, getSelectedAuthorities()));
        groups = null;
    }

    private void removeIncludedGroups(Group group, Map<String, Group> groups) {
        Set<String> groupNames = new HashSet<>(groups.keySet());
        for (String checkGroupName : groupNames) {
            if (!group.getName().equals(checkGroupName) && group.hasPrivilege(checkGroupName)) {
                Group includedGroup = groups.get(checkGroupName);
                if (includedGroup != null) {
                    removeIncludedGroups(includedGroup, groups);
                    groups.remove(checkGroupName);
                }
            }
        }
    }

    private void removeIncludedPrivileges(List<String> privileges, Map<String, Group> groups) {
        for (String privilege : new ArrayList<>(privileges)) {
            for (Group group : groups.values()) {
                if (group.hasPrivilege(privilege)) {
                    privileges.remove(privilege);
                }
            }
        }
    }

    public boolean isOnlyAdmin(Group objGroup) {
        if (!objGroup.hasPrivilege(Privileges.ADMIN.name())) {
            return false;
        }
        List<Group> groups = getGroups();
        int i = 0;
        for (Group group : groups) {
            if (group.hasPrivilege(Privileges.ADMIN.name())) {
                i++;
            }
        }
        return i <= 1;
    }

    public void deleteGroup(String name) {
        groupManagementService.deleteGroup(name);
        groups = null;
    }

    public void setGroupManagementService(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

}
