package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

// TODO Needs performance optimization
/**
 * @author Andrei Astrouski
 */
@Service
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

    public void addGroup() {
        groupManagementService.addGroup(name, description);
        updatePriveleges(name);
        groups = null;
    }

    public void editGroup() {
        groupManagementService.updateGroup(oldName, newName, description);
        updatePriveleges(newName);
        this.groups = null;
    }

    private void updatePriveleges(String name) {
        String[] privilegesParam = ((ServletRequest) WebStudioUtils.getExternalContext().getRequest())
            .getParameterValues("privilege");
        Set<String> privileges = new HashSet<>(
            Arrays.asList(privilegesParam == null ? new String[0] : privilegesParam));
        String[] groupNames = ((ServletRequest) WebStudioUtils.getExternalContext().getRequest())
            .getParameterValues("group");
        Set<String> groups = new HashSet<>(Arrays.asList(groupNames == null ? new String[0] : groupNames));
        getGroups().stream()
            .filter(gr -> groups.contains(gr.getName()))
            .flatMap(group -> group.getPrivileges().stream())
            .map(Privilege::getName)
            .forEach(pr -> privileges.remove(pr));
        groupManagementService.updateGroup(name, groups, privileges);
    }

    private boolean groupWasBefore(Group oldGroup, String checkGroupName) {
        return oldGroup != null && !oldGroup.hasGroup(checkGroupName);
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
