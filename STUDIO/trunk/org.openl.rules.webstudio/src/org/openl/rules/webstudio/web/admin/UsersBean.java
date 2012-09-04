package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.rules.security.User;
import org.openl.rules.webstudio.service.UserManagementService;

/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class UsersBean {

    @ManagedProperty(value="#{userManagementService}")
    private UserManagementService userManagementService;

    public List<User> getUsers() {
        return userManagementService.getAllUsers();
    }

    public void deleteUser(String username) {
        userManagementService.deleteUser(username);
    }

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

}
