package org.openl.rules.rest;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.GroupModel;
import org.openl.rules.rest.model.GroupType;
import org.openl.rules.rest.model.UserCreateModel;
import org.openl.rules.rest.model.UserEditModel;
import org.openl.rules.rest.model.UserModel;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.security.AdminPrivilege;
import org.openl.rules.security.Group;
import org.openl.rules.security.OwnerOrAdminPrivilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.security.acl.JdbcMutableAclService;
import org.openl.util.StreamUtils;
import org.openl.util.StringUtils;

@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnExpression("'${user.mode}' != 'single'")
@Tag(name = "Users")
public class UsersManagementController {

    private final UserManagementService userManagementService;
    private final Boolean canCreateInternalUsers;
    private final AdminUsers adminUsersInitializer;
    private final CurrentUserInfo currentUserInfo;
    private final BeanValidationProvider validationProvider;
    private final PasswordEncoder passwordEncoder;
    private final ExternalGroupService extGroupService;
    private final MailSender mailSender;
    private final JdbcMutableAclService aclService;

    @Autowired
    public UsersManagementController(UserManagementService userManagementService,
                                     Boolean canCreateInternalUsers,
                                     AdminUsers adminUsersInitializer,
                                     CurrentUserInfo currentUserInfo,
                                     PasswordEncoder passwordEncoder,
                                     BeanValidationProvider validationService,
                                     ExternalGroupService extGroupService,
                                     MailSender mailSender,
                                     @Autowired(required = false) JdbcMutableAclService aclService) {
        this.userManagementService = userManagementService;
        this.canCreateInternalUsers = canCreateInternalUsers;
        this.adminUsersInitializer = adminUsersInitializer;
        this.currentUserInfo = currentUserInfo;
        this.passwordEncoder = passwordEncoder;
        this.validationProvider = validationService;
        this.extGroupService = extGroupService;
        this.mailSender = mailSender;
        this.aclService = aclService;
    }

    @Operation(description = "users.get-users.desc", summary = "users.get-users.summary")
    @GetMapping
    public List<UserModel> getAllUsers() {
        return userManagementService.getAllUsers().stream().map(this::mapUser).collect(Collectors.toList());
    }

    @Operation(description = "users.get-user.desc", summary = "users.get-user.summary")
    @GetMapping("/{username}")
    @OwnerOrAdminPrivilege
    public UserModel getUser(
            @Parameter(description = "users.field.username") @PathVariable("username") String username) {
        checkUserExists(username);
        return Optional.ofNullable(userManagementService.getUser(username)).map(this::mapUser).orElse(null);
    }

    @Operation(description = "users.add-user.desc", summary = "users.add-user.summary")
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AdminPrivilege
    public void addUser(HttpServletRequest request, @RequestBody UserCreateModel userModel) {
        validationProvider.validate(userModel);
        userManagementService.addUser(userModel.getUsername(),
                userModel.getFirstName(),
                userModel.getLastName(),
                canCreateInternalUsers ? userModel.getInternalPassword().getPassword() : null,
                userModel.getEmail(),
                userModel.getDisplayName());
        userManagementService.updateAuthorities(userModel.getUsername(), userModel.getGroups());
        if (StringUtils.isNotBlank(userModel.getEmail())) {
            mailSender.sendVerificationMail(userManagementService.getUser(userModel.getUsername()), request);
        }
    }

    @Operation(description = "users.edit-user.desc", summary = "users.edit-user.summary")
    @PutMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @OwnerOrAdminPrivilege
    public void editUser(HttpServletRequest request,
                         @RequestBody UserEditModel userModel,
                         @Parameter(description = "users.field.username") @PathVariable("username") String username) {
        checkUserExists(username);
        validationProvider.validate(userModel);
        User dbUser = userManagementService.getUser(username);
        boolean emailChanged = !Objects.equals(dbUser.getEmail(), userModel.getEmail()) && !dbUser.getExternalFlags()
                .isEmailExternal();
        userManagementService.updateUserData(username,
                userModel.getFirstName(),
                userModel.getLastName(),
                userModel.getPassword(),
                userModel.getEmail(),
                userModel.getDisplayName(),
                !emailChanged && dbUser.getExternalFlags().isEmailVerified());
        boolean leaveAdminGroups = adminUsersInitializer.isSuperuser(username) || Objects
                .equals(currentUserInfo.getUserName(), username);
        userManagementService.updateAuthorities(username, userModel.getGroups(), leaveAdminGroups);

        if (StringUtils.isNotBlank(userModel.getEmail()) && emailChanged) {
            mailSender.sendVerificationMail(userManagementService.getUser(username), request);
        }
    }

    @Operation(description = "users.delete-user.desc", summary = "users.delete-user.summary")
    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@Parameter(description = "users.field.username") @PathVariable("username") String username) {
        checkUserExists(username);
        userManagementService.deleteUser(username);
        if (aclService != null) {
            aclService.deleteSid(new PrincipalSid(username));
        }
    }

    @Operation(description = "users.get-user-external-groups.desc", summary = "users.get-user-external-groups.summary")
    @GetMapping("/{username}/groups/external")
    @AdminPrivilege
    public Set<String> getUserExternalGroups(
            @Parameter(description = "users.field.username") @PathVariable("username") String username,
            @Parameter(description = "users.get-user-external-groups.param.matched.username") @RequestParam(value = "matched", required = false) Boolean matched) {
        checkUserExists(username);
        List<Group> extGroups;
        if (matched == null) {
            extGroups = extGroupService.findAllForUser(username);
        } else if (matched) {
            extGroups = extGroupService.findMatchedForUser(username);
        } else {
            extGroups = extGroupService.findNotMatchedForUser(username);
        }
        return extGroups.stream().map(Group::getName).collect(StreamUtils.toTreeSet(String.CASE_INSENSITIVE_ORDER));
    }

    private UserModel mapUser(User user) {
        List<Group> extGroups = extGroupService.findMatchedForUser(user.getUsername());
        Stream<GroupModel> matchedExtGroupsStream = extGroups.stream()
                .map(simpleGroup -> new GroupModel().setName(simpleGroup.getName())
                        .setType(simpleGroup.hasPrivilege(Privileges.ADMIN.name()) ? GroupType.ADMIN : GroupType.EXTERNAL));
        Stream<GroupModel> internalGroupStream = user.getAuthorities()
                .stream()
                .map(SimpleGroup.class::cast)
                // resolve collisions when the same group external and internal
                .filter(g -> extGroups.stream().noneMatch(ext -> Objects.equals(ext.getName(), g.getName())))
                .map(simpleGroup -> new GroupModel().setName(simpleGroup.getName())
                        .setType(simpleGroup.hasPrivilege(Privileges.ADMIN.name()) ? GroupType.ADMIN : GroupType.DEFAULT));

        long cntNotMatchedExtGroups = extGroupService.countNotMatchedForUser(user.getUsername());
        return new UserModel().setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setEmail(user.getEmail())
                .setUserGroups(Stream.concat(matchedExtGroupsStream, internalGroupStream)
                        .collect(StreamUtils.toTreeSet(Comparator.comparing(GroupModel::getType)
                                .thenComparing(GroupModel::getName, String.CASE_INSENSITIVE_ORDER))))
                .setUsername(user.getUsername())
                .setCurrentUser(currentUserInfo.getUserName().equals(user.getUsername()))
                .setSuperUser(adminUsersInitializer.isSuperuser(user.getUsername()))
                .setUnsafePassword(
                        user.getPassword() != null && passwordEncoder.matches(user.getUsername(), user.getPassword()))
                .setNotMatchedExternalGroupsCount(cntNotMatchedExtGroups)
                .setDisplayName(user.getDisplayName())
                .setOnline(userManagementService.isUserOnline(user.getUsername()))
                .setExternalFlags(user.getExternalFlags());
    }

    private void checkUserExists(String username) {
        if (!userManagementService.existsByName(username)) {
            throw new NotFoundException("users.message", username);
        }
    }
}
