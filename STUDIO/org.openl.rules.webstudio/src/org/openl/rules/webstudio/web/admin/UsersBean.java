package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * @author Andrei Astrouski
 */
@Service
@RequestScope
public class UsersBean {

    public static final String VALIDATION_EMPTY = "Cannot be empty";
    public static final String VALIDATION_MAX = "Must be less than 25";
    public static final String VALIDATION_USERNAME_CHARACTERS = "The name must not contain the following characters: / \\ : * ? \" < > | { } ~ ^";
    public static final String VALIDATION_USERNAME_BEGIN_END = "The name should not end or begin with '.' or a whitespace.";
    public static final String VALIDATION_USERNAME_CONSECUTIVE = "The name should not contain consecutive '.'.";

    @Size(max = 25, message = VALIDATION_MAX)
    private String firstName;

    @Size(max = 25, message = VALIDATION_MAX)
    private String lastName;

    @NotBlank(message = VALIDATION_EMPTY)
    @Size(max = 25, message = VALIDATION_MAX)
    @Pattern.List({ @Pattern(regexp = "(.(?<![.]{2}))+", message = VALIDATION_USERNAME_CONSECUTIVE),
            @Pattern(regexp = "[^.\\s].*[^.\\s]|[^.\\s]", message = VALIDATION_USERNAME_BEGIN_END),
            @Pattern(regexp = "[^\\/\\\\:*?\"<>|{}~^]*", message = VALIDATION_USERNAME_CHARACTERS) })
    private String username;

    @NotBlank(message = VALIDATION_EMPTY)
    @Size(max = 25, message = VALIDATION_MAX)
    private String password;

    @Size(max = 25, message = VALIDATION_MAX)
    private String changedPassword;

    private Set<String> groups;

    private boolean internalUser = false;

    @Autowired
    protected UserManagementService userManagementService;

    @Autowired
    protected GroupManagementService groupManagementService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Value("#{canCreateInternalUsers}")
    protected boolean canCreateInternalUsers;

    @Value("#{canCreateExternalUsers}")
    protected boolean canCreateExternalUsers;

    @Autowired
    protected AdminUsers adminUsersInitializer;

    @Autowired
    protected CurrentUserInfo currentUserInfo;

    /**
     * Validation for existed user
     */
    public void validateUsername(FacesContext context, UIComponent toValidate, Object value) {
        User user = userManagementService.loadUserByUsername((String) value);

        if (user != null) {
            throw new ValidatorException(new FacesMessage("User with such name already exists"));
        }
    }

    public List<User> getUsers() {
        return userManagementService.getAllUsers();
    }

    public String getGroups(Object objUser) {
        @SuppressWarnings("unchecked")
        Collection<Privilege> authorities = (Collection<Privilege>) ((User) objUser).getAuthorities();
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Privilege authority : authorities) {
            if (authority instanceof Group) {
                joiner.add("\"" + authority.getName() + "\"");
            }
        }
        return joiner.toString();
    }

    public String getOnlyAdminGroups() {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Group x : groupManagementService.getGroups()) {
            if (x.hasPrivilege(Privileges.ADMIN.getAuthority())) {
                joiner.add("\"" + x.getAuthority() + "\"");
            }
        }
        return joiner.toString();
    }

    public void addUser() {
        boolean willBeExternalUser = canCreateExternalUsers && (!internalUser || !canCreateInternalUsers);
        String passwordHash = willBeExternalUser ? null : passwordEncoder.encode(password);
        userManagementService.addUser(username, firstName, lastName, passwordHash);
        userManagementService.updateAuthorities(username, groups);
    }

    public void editUser() {
        User user = userManagementService.loadUserByUsername(username);
        if (user.isInternalUser()) {
            boolean updatePassword = StringUtils.isNotBlank(changedPassword);
            String passwordHash = updatePassword ? passwordEncoder.encode(changedPassword) : null;
            userManagementService.updateUserData(username, firstName, lastName, passwordHash, updatePassword);
        }
        boolean leaveAdminGroups = adminUsersInitializer.isSuperuser(username) || currentUserInfo.getUserName()
            .equals(username);
        userManagementService.updateAuthorities(username, groups, leaveAdminGroups);
    }

    public boolean isSuperuser(User user) {
        String username = user.getUsername();
        return adminUsersInitializer.isSuperuser(username);
    }

    public boolean isCurrentUser(User user) {
        String username = user.getUsername();
        return currentUserInfo.getUserName().equals(username);
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

    public boolean isUnsafePassword(User user) {
        String password = user.getPassword();
        String username = user.getUsername();
        return password != null && passwordEncoder.matches(username, password);
    }

    public void setInternalUser(boolean internalUser) {
        this.internalUser = internalUser;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
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
