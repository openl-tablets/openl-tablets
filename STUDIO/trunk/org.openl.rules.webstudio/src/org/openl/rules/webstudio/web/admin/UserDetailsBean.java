package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Collection;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;

@ManagedBean
@RequestScoped
public class UserDetailsBean extends UsersBean {
    private User user;
    private String newPassword;
    private String confirmPassword;
    private CurrentUserInfo userInfo;
    private org.openl.rules.security.User simpleUser;
    private boolean isPasswordValid = false;

    public UserDetailsBean() {
        super();
    }

    /**
     * Returns the current logged in user
     * 
     * @return org.openl.rules.security.User
     */
    public User getUser() {
        userInfo = new CurrentUserInfo();
        setUsername(userInfo.getUser().getUsername());
        user = userManagementService.loadUserByUsername(userInfo.getUser().getUsername());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());

        return user;
    }

    /**
     * Return the user's privileges
     * 
     * @return Collection of user's privileges
     */
    private Collection<Privilege> getPriveleges() {
        Collection<Privilege> privileges = new ArrayList<Privilege>();

        for (GrantedAuthority auth : user.getAuthorities()) {
            Privilege group = (Privilege) groupManagementService.getGroupByName(auth.getAuthority());
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
            setPassword(newPassword);
        }

        simpleUser = new SimpleUser(getFirstName(), getLastName(), getUsername(), getPassword(), getPriveleges());
        userManagementService.updateUser(simpleUser);
    }

    /**
     * Validates newPassword and confirmPassword on identity. If newPassword
     * isEmty, then validation isn't needed
     * 
     * @param context
     * @param component
     * @param value
     */
    public void passwordsValidator(FacesContext context, UIComponent component, Object value) {
        newPassword = (String) value;

        if (!StringUtils.isEmpty(newPassword)) {
            UIInput uiInputConfirmPassword = (UIInput) component.getAttributes().get("confirmPassword");
            String confirmPasswordString = uiInputConfirmPassword.getSubmittedValue().toString();

            if (!StringUtils.equals(newPassword, confirmPasswordString)) {
                throw new ValidatorException(new FacesMessage("Password missmatch"));
            } else {
                isPasswordValid = true;
            }
        }
    }

    /**
     * Validates the entered password is correct or not
     * 
     * @param context
     * @param toValidate
     * @param value
     */
    public void currentPassValidator(FacesContext context, UIComponent toValidate, Object value) {

        String userPasswordHash = user.getPassword();
        String enteredPasswordHash = new Md5PasswordEncoder().encodePassword((String) value, null);

        if (StringUtils.isEmpty((String) value)) {
            throw new ValidatorException(new FacesMessage("Enter your password"));
        }

        if (!userPasswordHash.equals(enteredPasswordHash)) {
            throw new ValidatorException(new FacesMessage("Incorect password!"));
        }
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

}