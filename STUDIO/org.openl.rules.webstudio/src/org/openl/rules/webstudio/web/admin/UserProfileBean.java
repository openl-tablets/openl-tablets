package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.validator.ValidatorException;
import javax.validation.constraints.Size;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ManagedBean
@RequestScoped
public class UserProfileBean extends UsersBean {
    public static final String VALIDATION_MAX = "Must be less than 25";
    private final Logger log = LoggerFactory.getLogger(UserProfileBean.class);

    private User user;
    private String newPassword;
    private String confirmPassword;
    @ManagedProperty(value = "#{currentUserInfo}")
    private CurrentUserInfo currentUserInfo;
    private org.openl.rules.security.User simpleUser;
    private boolean isPasswordValid = false;
    private String currentPassword;
    private String userPassword;

    @Size(max = 25, message = VALIDATION_MAX)
    private String userFirstName;

    @Size(max = 25, message = VALIDATION_MAX)
    private String userLastName;

    public UserProfileBean() {
        super();
    }

    @PostConstruct
    public void initialize() {
        setUsername(currentUserInfo.getUserName());
        Authentication authentication = currentUserInfo.getAuthentication();
        if (authentication.getPrincipal() instanceof User) {
            user = (User) authentication.getPrincipal();
        } else if (authentication.getDetails() instanceof User) {
            user = (User) authentication.getDetails();
        } else {
            try {
                user = userManagementService.loadUserByUsername(getUsername());
            } catch (UsernameNotFoundException e) {
                log.warn("User details for user '" + getUsername() + "' can't be retrieved.");
                user = new SimpleUser(null, null, getUsername(), null, Collections.<Privilege> emptyList());
            }
        }
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        setCurrentPassword(user.getPassword());
        setInternalUser(user.isInternalUser());
    }

    /**
     * Returns the current logged in user
     *
     * @return org.openl.rules.security.User
     */
    public User getUser() {
        return user;
    }

    /**
     * Return the user's privileges
     *
     * @return Collection of user's privileges
     */
    private Collection<Privilege> getPriveleges() {
        Collection<Privilege> privileges = new ArrayList<>();

        for (GrantedAuthority auth : user.getAuthorities()) {
            Privilege group = groupManagementService.getGroupByName(auth.getAuthority());
            privileges.add(group);
        }
        return privileges;
    }

    /**
     * Updates the user's firstName, lastName, passWord
     */
    @Override
    public void editUser() {

        if (isPasswordValid) {
            String encodedPassword = passwordEncoder.encode(newPassword);
            setCurrentPassword(encodedPassword);
        } else {
            setCurrentPassword(getUser().getPassword());
        }

        if (userFirstName == null) {
            userFirstName = getFirstName();
        }

        if (userLastName == null) {
            userLastName = getLastName();
        }

        simpleUser = new SimpleUser(getUserFirstName(),
            getUserLastName(),
            getUsername(),
            currentPassword,
            getPriveleges());
        userManagementService.updateUser(simpleUser);

        Authentication authentication = currentUserInfo.getAuthentication();
        SimpleUser user = null;
        if (authentication.getPrincipal() instanceof SimpleUser) {
            user = (SimpleUser) authentication.getPrincipal();
        } else if (authentication.getDetails() instanceof SimpleUser) {
            user = (SimpleUser) authentication.getDetails();
        }
        if (user != null) {
            user.setFirstName(userFirstName);
            user.setLastName(userLastName);
            user.setPassword(currentPassword);
        }
    }

    /**
     * Validates newPassword and confirmPassword on identity. If newPassword isEmty, then validation isn't needed
     */
    public void passwordsValidator(FacesContext context, UIComponent component, Object value) {
        newPassword = (String) value;

        if (StringUtils.isNotEmpty(newPassword)) {
            UIInput uiInputConfirmPassword = (UIInput) component.getAttributes().get("confirmPassword");
            String confirmPasswordString = uiInputConfirmPassword.getSubmittedValue().toString();
            UIInput uiInputPassword = (UIInput) component.getAttributes().get("currentPassword");
            String passwordString = uiInputPassword.getValue().toString();
            String userPasswordHash = user.getPassword();

            if (StringUtils.isEmpty(passwordString)) {
                throw new ValidatorException(new FacesMessage("Enter your password"));
            }

            if (!newPassword.equals(confirmPasswordString)) {
                throw new ValidatorException(new FacesMessage("New password and confirm password do not match."));
            } else {
                isPasswordValid = true;
            }

            if (!passwordEncoder.matches(passwordString, userPasswordHash)) {
                throw new ValidatorException(new FacesMessage("Incorrect current password!"));
            }
        }
    }

    public void firstNameListener(ValueChangeEvent e) {
        UIInput uiInput = (UIInput) e.getComponent();
        setUserFirstName(uiInput.getValue().toString());
    }

    public void lastNameListener(ValueChangeEvent e) {
        UIInput uiInput = (UIInput) e.getComponent();
        setUserLastName(uiInput.getValue().toString());
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public org.openl.rules.security.User getSimpleUser() {
        return simpleUser;
    }

    public void setSimpleUser(org.openl.rules.security.User simpleUser) {
        this.simpleUser = simpleUser;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public void setCurrentUserInfo(CurrentUserInfo currentUserInfo) {
        this.currentUserInfo = currentUserInfo;
    }
}