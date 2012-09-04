package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.service.UserManagementService;

/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class UsersBean {

    private User user = new SimpleUser();
    private String confirmPassword;

    @ManagedProperty(value="#{userManagementService}")
    private UserManagementService userManagementService;

    public List<User> getUsers() {
        return userManagementService.getAllUsers();
    }

    public void addUser() {
        userManagementService.addUser(user);
    }

    public void deleteUser(String username) {
        userManagementService.deleteUser(username);
    }

    public User getUser() {
        return user;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

}
