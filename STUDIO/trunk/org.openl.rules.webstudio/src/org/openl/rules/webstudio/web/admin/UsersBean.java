package org.openl.rules.webstudio.web.admin;

import java.util.Collection;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Size;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.ScriptAssert;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.service.UserManagementService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
@ScriptAssert(lang = "javascript", script = "_this.confirmPassword.equals(_this.password)", message="The password fields must match")
public class UsersBean {

    @Size(max=25)
    private String firstName;

    @Size(max=25)
    private String lastName;

    @NotBlank(message="Can not be empty")
    @Size(max=25)
    private String username;

    @NotBlank(message="Can not be empty")
    @Size(max=25)
    private String password;

    @NotBlank(message="Can not be empty")
    private String confirmPassword;

    private Collection<Privilege> privileges;

    @ManagedProperty(value="#{userManagementService}")
    private UserManagementService userManagementService;

    /**
     * Validation for existed user
     */
    public void validateUsername(FacesContext context, UIComponent toValidate, Object value) {
        User user = null;
        try {
            user = userManagementService.loadUserByUsername((String) value);
        } catch (UsernameNotFoundException e) { }

        if (user != null) {
            throw new ValidatorException(
                    new FacesMessage("User with such name already exists"));
        }
    }

    /**
     * Validation for password confirmation
     */
    public void validateConfirmPassword(FacesContext context, UIComponent toValidate, Object value) {
        String confirmPassword = (String) value;
        if (StringUtils.isNotBlank(password) && StringUtils.isNotBlank(confirmPassword)
                && !confirmPassword.equals(password)) {
            throw new ValidatorException(
                    new FacesMessage("Confirm password does not match the password"));
        }
    }

    public List<User> getUsers() {
        return userManagementService.getAllUsers();
    }

    public void addUser() {
        userManagementService.addUser(
                new SimpleUser(firstName, lastName, username, password, privileges));
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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Collection<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Collection<Privilege> privileges) {
        this.privileges = privileges;
    }

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

}
