package org.openl.rules.webstudio.web.admin;

import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.openl.rules.security.*;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.util.StringUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class UsersBean {

    public static final String VALIDATION_EMPTY = "Cannot be empty";
    public static final String VALIDATION_MAX = "Must be less than 25";
    public static final String VALIDATION_USERNAME = "Invalid characters (valid: latin letters, numbers, _ and -)";
    public static final String VALIDATION_GROUPS = "Please select at least one group";

    @Size(max = 25, message = VALIDATION_MAX)
    private String firstName;

    @Size(max = 25, message = VALIDATION_MAX)
    private String lastName;

    @NotBlank(message = VALIDATION_EMPTY)
    @Size(max = 25, message = VALIDATION_MAX)
    @Pattern(regexp = "([a-zA-Z0-9-_]*)?", message = VALIDATION_USERNAME)
    private String username;

    @NotBlank(message = VALIDATION_EMPTY)
    @Size(max = 25, message = VALIDATION_MAX)
    private String password;

    @Size(max = 25, message = VALIDATION_MAX)
    private String changedPassword;

    @NotEmpty(message = VALIDATION_GROUPS)
    private List<String> groups;

    private boolean internalUser = false;

    @ManagedProperty(value = "#{userManagementService}")
    protected UserManagementService userManagementService;

    @ManagedProperty(value = "#{groupManagementService}")
    protected GroupManagementService groupManagementService;

    @ManagedProperty(value = "#{passwordEncoder}")
    protected PasswordEncoder passwordEncoder;

    @ManagedProperty(value = "#{canCreateInternalUsers}")
    protected boolean canCreateInternalUsers;

    @ManagedProperty(value = "#{canCreateExternalUsers}")
    protected boolean canCreateExternalUsers;

    /**
     * Validation for existed user
     */
    public void validateUsername(FacesContext context, UIComponent toValidate, Object value) {
        User user = null;
        try {
            user = userManagementService.loadUserByUsername((String) value);
        } catch (UsernameNotFoundException ignored) {
        }

        if (user != null) {
            throw new ValidatorException(new FacesMessage("User with such name already exists"));
        }
    }

    public List<User> getUsers() {
        return userManagementService.getAllUsers();
    }

    public String[] getGroups(Object objUser) {
        List<String> groups = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Collection<Privilege> authorities = (Collection<Privilege>) ((User) objUser).getAuthorities();
        for (Privilege authority : authorities) {
            if (authority instanceof Group) {
                groups.add(authority.getName());
            }
        }
        return groups.toArray(new String[groups.size()]);
    }

    public String[] getOnlyAdminGroups(Object objUser) {
        if (!isOnlyAdmin(objUser)) {
            return new String[0];
        }

        String adminPrivilege = Privileges.ADMIN.name();

        List<String> groups = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Collection<Privilege> authorities = (Collection<Privilege>) ((User) objUser).getAuthorities();
        for (Privilege authority : authorities) {
            if (authority instanceof Group) {
                Group group = (Group) authority;
                if (group.hasPrivilege(adminPrivilege)) {
                    groups.add(group.getAuthority());
                }
            }
        }

        return groups.toArray(new String[groups.size()]);
    }

    private List<Privilege> getSelectedGroups() {
        List<Privilege> resultGroups = new ArrayList<>();
        Map<String, Group> groups = new HashMap<>();

        if (this.groups != null) {
            for (String groupName : this.groups) {
                groups.put(groupName, groupManagementService.getGroupByName(groupName));
            }

            for (Group group : new ArrayList<>(groups.values())) {
                if (!groups.isEmpty()) {
                    removeIncludedGroups(group, groups);
                }
            }

            resultGroups.addAll(groups.values());
        }

        return resultGroups;
    }

    public void addUser() {
        boolean willBeExternalUser = canCreateExternalUsers && (!internalUser || !canCreateInternalUsers);
        String passwordHash = willBeExternalUser ? null : passwordEncoder.encode(password);
        userManagementService.addUser(new SimpleUser(firstName, lastName, username, passwordHash, getSelectedGroups()));
    }

    public void editUser() {
        User user = userManagementService.loadUserByUsername(username);
        if (!user.isInternalUser()) {
            firstName = user.getFirstName();
            lastName = user.getLastName();
        }
        String passwordHash = StringUtils.isBlank(changedPassword) ? null : passwordEncoder.encode(changedPassword);
        userManagementService
            .updateUser(new SimpleUser(firstName, lastName, username, passwordHash, getSelectedGroups()));
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

    public boolean isOnlyAdmin(Object objUser) {
        String adminPrivilege = Privileges.ADMIN.name();
        return ((User) objUser)
            .hasPrivilege(adminPrivilege) && userManagementService.getUsersByPrivilege(adminPrivilege).size() == 1;
    }

    public void deleteUser(String username) {
        userManagementService.deleteUser(username);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getChangedPassword() {
        return changedPassword;
    }

    public void setChangedPassword(String changedPassword) {
        this.changedPassword = changedPassword;
    }

    public boolean isInternalUser() {
        return internalUser;
    }

    public void setInternalUser(boolean internalUser) {
        this.internalUser = internalUser;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public List<SelectItem> getGroupItems() {
        List<SelectItem> result = new ArrayList<>();
        List<Group> groups = groupManagementService.getGroups();
        for (Group group : groups) {
            result.add(new SelectItem(group.getName(), group.getDisplayName()));
        }
        return result;
    }

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    public void setGroupManagementService(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void setCanCreateExternalUsers(boolean canCreateExternalUsers) {
        this.canCreateExternalUsers = canCreateExternalUsers;
    }

    public void setCanCreateInternalUsers(boolean canCreateInternalUsers) {
        this.canCreateInternalUsers = canCreateInternalUsers;
    }

    public boolean isCanCreateInternalUsers() {
        return canCreateInternalUsers;
    }

    public boolean isCanCreateUsers() {
        return canCreateInternalUsers || canCreateExternalUsers;
    }

    /**
     * Returns true if both internal and external users are supported. Returns false if only internal or only external
     * users are supported.
     */
    public boolean isCanSelectInternalOrExternal() {
        return canCreateInternalUsers && canCreateExternalUsers;
    }
}
