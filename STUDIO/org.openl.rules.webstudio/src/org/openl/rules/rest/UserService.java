package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.rules.rest.model.UserDTO;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.PrivilegesEvaluator;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
public class UserService {

    private final UserManagementService userManagementService;
    private final Boolean canCreateInternalUsers;
    private final Boolean canCreateExternalUsers;
    private final AdminUsers adminUsersInitializer;
    private final CurrentUserInfo currentUserInfo;
    protected PasswordEncoder passwordEncoder;

    @Inject
    public UserService(UserManagementService userManagementService,
            Boolean canCreateInternalUsers,
            Boolean canCreateExternalUsers,
            AdminUsers adminUsersInitializer,
            CurrentUserInfo currentUserInfo,
            PasswordEncoder passwordEncoder) {
        this.userManagementService = userManagementService;
        this.canCreateInternalUsers = canCreateInternalUsers;
        this.canCreateExternalUsers = canCreateExternalUsers;
        this.adminUsersInitializer = adminUsersInitializer;
        this.currentUserInfo = currentUserInfo;
        this.passwordEncoder = passwordEncoder;
    }

    @GET
    public List<UserDTO> getAllUsers() {
        List<org.openl.rules.security.standalone.persistence.User> allUsers = userManagementService.getAllUsers();
        List<UserDTO> resultUsers = new ArrayList<>();
        for (org.openl.rules.security.standalone.persistence.User user : allUsers) {
            UserDTO resultUser = new UserDTO(user.getFirstName(),
                user.getSurname(),
                user.getLoginName(),
                user.getEmail(),
                user.getPasswordHash(),
                PrivilegesEvaluator.createPrivileges(user));
            resultUser.setCurrentUser(currentUserInfo.getUserName().equals(user.getLoginName()));
            resultUser.setSuperUser(adminUsersInitializer.isSuperuser(user.getLoginName()));
            resultUser.setUnsafePassword(resultUser.getPassword() != null && passwordEncoder
                .matches(user.getLoginName(), resultUser.getPassword()));
            resultUsers.add(resultUser);
        }
        return resultUsers;
    }

    @GET
    @Path("/{name}")
    public User loadUserByUsername(String name) {
        return userManagementService.loadUserByUsername(name);
    }

    @POST
    public void addUser(UserDTO user) {
        boolean willBeExternalUser = canCreateExternalUsers && (!user.isInternalUser() || !canCreateInternalUsers);
        String passwordHash = willBeExternalUser ? null : passwordEncoder.encode(user.getPassword());
        userManagementService
            .addUser(user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail(), passwordHash);
        userManagementService.updateAuthorities(user.getUsername(), user.getGroups());
    }

    @PUT
    public Response editUser(UserDTO user) {
        if (user.isInternalUser()) {
            boolean updatePassword = StringUtils.isNotBlank(user.getPassword());
            String passwordHash = updatePassword ? passwordEncoder.encode(user.getPassword()) : null;
            userManagementService.updateUserData(user.getUsername(), user.getFirstName(), user.getLastName(), passwordHash, updatePassword);
        }
        boolean leaveAdminGroups = adminUsersInitializer.isSuperuser(user.getUsername()) || currentUserInfo.getUserName()
            .equals(user.getUsername());
        userManagementService.updateAuthorities(user.getUsername(), user.getGroups(), leaveAdminGroups);
        return Response.ok().build();
    }

    @DELETE
    public void deleteUser(String userName) {
        userManagementService.deleteUser(userName);
    }

    @GET
    @Path("/options")
    public Map<String, Object> options() {
        HashMap<String, Object> options = new HashMap<>();
        options.put("canCreateInternalUsers", canCreateInternalUsers);
        options.put("canCreateExternalUsers", canCreateExternalUsers);
        return options;
    }

}
