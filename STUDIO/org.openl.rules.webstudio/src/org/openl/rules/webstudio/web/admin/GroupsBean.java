package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
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
import org.openl.util.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.RequestScope;

// TODO Needs performance optimization
/**
 * @author Andrei Astrouski
 */
@Controller
@RequestScope
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

    public GroupsBean(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

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

    private final GroupManagementService groupManagementService;

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

        String[] privilegesParam = ((ServletRequest) WebStudioUtils.getExternalContext().getRequest())
            .getParameterValues("privilege");
        List<String> privileges = new ArrayList<>(
            Arrays.asList(privilegesParam == null ? new String[0] : privilegesParam));
        privileges.add(0, Privileges.VIEW_PROJECTS.name());

        // Admin
        Map<String, Group> groups = new java.util.HashMap<>();
        String[] groupNames = ((ServletRequest) WebStudioUtils.getExternalContext().getRequest())
            .getParameterValues("group");
        if (groupNames != null) {
            for (String groupName : groupNames) {
                if (groupName.equals(oldName)) {
                    // Persisting group should not include itself
                    continue;
                }
                groups.put(groupName, groupManagementService.getGroupByName(groupName));
            }

            Group oldGroup = null;
            if (StringUtils.isNotBlank(oldName)) {
                oldGroup = groupManagementService.getGroupByName(oldName);
            }
            /*
              Trying to find super group, which includes all the subgroups. If there is an old group -> no need to
              remove its subgroups - it's needed to store them.
             */
            for (Group group : new ArrayList<>(groups.values())) {
                if (!groups.isEmpty()) {
                    removeIncludedGroups(group, groups, oldGroup);
                }
            }
            // when old group exists and there are super groups, which includes it -> remove them to prevent the cycle
            if (StringUtils.isNotBlank(oldName)) {
                for (Group value : new ArrayList<>(groups.values())) {
                    if (value.hasPrivilege(oldName)) {
                        groups.remove(value.getName());
                    }
                }
            }

            removeIncludedPrivileges(privileges, groups);

            authorities.addAll(groups.values());
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

    private void removeIncludedGroups(Group group, Map<String, Group> groups, Group oldGroup) {
        Set<String> groupNames = new HashSet<>(groups.keySet());
        for (String checkGroupName : groupNames) {
            if (!group.getName().equals(checkGroupName) && group
                .hasPrivilege(checkGroupName) && groupWasBefore(oldGroup, checkGroupName)) {
                Group includedGroup = groups.get(checkGroupName);
                if (includedGroup != null) {
                    removeIncludedGroups(includedGroup, groups, oldGroup);
                    groups.remove(checkGroupName);
                }
            }
        }
    }

    private boolean groupWasBefore(Group oldGroup, String checkGroupName) {
        return oldGroup != null && !oldGroup.hasGroup(checkGroupName);
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

}
