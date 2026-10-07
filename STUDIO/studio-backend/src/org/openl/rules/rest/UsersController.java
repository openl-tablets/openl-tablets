package org.openl.rules.rest;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.ObjectUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.rest.model.GroupModel;
import org.openl.rules.rest.model.GroupType;
import org.openl.rules.rest.model.UserCreateModel;
import org.openl.rules.rest.model.UserEditModel;
import org.openl.rules.rest.model.UserInfoEditModel;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.rest.model.UserModel;
import org.openl.rules.rest.model.UserProfileEditModel;
import org.openl.rules.rest.model.UserProfileModel;
import org.openl.rules.security.Group;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.security.acl.JdbcSidManagingAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.security.AdminPrivilege;
import org.openl.studio.security.CurrentUserInfo;
import org.openl.studio.security.OwnerOrAdminPrivilege;
import org.openl.studio.security.SecurityUtils;
import org.openl.util.StreamUtils;
import org.openl.util.StringUtils;

@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users")
public class UsersController {

    private final UserManagementService userManagementService;
    private final boolean canCreateInternalUsers;
    private final AdminUsers adminUsersInitializer;
    private final CurrentUserInfo currentUserInfo;
    private final BeanValidationProvider validationProvider;
    private final UserSettingManagementService userSettingsManager;
    private final PasswordEncoder passwordEncoder;
    private final ExternalGroupService extGroupService;
    private final MailSender mailSender;
    private final JdbcSidManagingAclService aclService;
    private final TransactionTemplate txTemplate;
    private final boolean groupsDisabled;

    @Autowired
    public UsersController(UserManagementService userManagementService,
                           Boolean canCreateInternalUsers,
                           AdminUsers adminUsersInitializer,
                           CurrentUserInfo currentUserInfo,
                           PasswordEncoder passwordEncoder,
                           BeanValidationProvider validationService,
                           UserSettingManagementService userSettingsManager,
                           ExternalGroupService extGroupService,
                           MailSender mailSender,
                           PlatformTransactionManager txManager,
                           @Autowired(required = false) JdbcSidManagingAclService aclService,
                           @Value("${user.mode}") String userMode) {
        this.userManagementService = userManagementService;
        this.canCreateInternalUsers = canCreateInternalUsers;
        this.adminUsersInitializer = adminUsersInitializer;
        this.currentUserInfo = currentUserInfo;
        this.passwordEncoder = passwordEncoder;
        this.userSettingsManager = userSettingsManager;
        this.validationProvider = validationService;
        this.extGroupService = extGroupService;
        this.mailSender = mailSender;
        this.aclService = aclService;
        this.txTemplate = new TransactionTemplate(txManager);
        this.groupsDisabled = "multi".equals(userMode) || "single".equals(userMode);
    }

    private void validateGroupsNotProvided(Set<String> groups) {
        if (groupsDisabled && groups != null && !groups.isEmpty()) {
            throw new BadRequestException("groups.not.supported.message");
        }
    }

    @Operation(description = "users.get-users.desc", summary = "users.get-users.summary")
    @GetMapping
    @AdminPrivilege
    public List<UserModel> getAllUsers() {
        return userManagementService.getAllUsers().stream().map(this::mapUser).toList();
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
    @AdminPrivilege
    public void addUser(HttpServletRequest request, @RequestBody UserCreateModel userModel) {
        validationProvider.validate(userModel);
        validateGroupsNotProvided(userModel.getGroups());
        userManagementService.addUser(userModel.getUsername(),
                userModel.getFirstName(),
                userModel.getLastName(),
                canCreateInternalUsers ? userModel.getInternalPassword().getPassword() : null,
                userModel.getEmail(),
                userModel.getDisplayName());
        if (!groupsDisabled) {
            userManagementService.updateAuthorities(userModel.getUsername(), userModel.getGroups());
        }
        if (StringUtils.isNotBlank(userModel.getEmail())) {
            mailSender.sendVerificationMail(userManagementService.getUser(userModel.getUsername()), request);
        }
    }

    @Operation(description = "users.edit-user.desc", summary = "users.edit-user.summary")
    @PutMapping("/{username}")
    @OwnerOrAdminPrivilege
    public void editUser(HttpServletRequest request,
                         @RequestBody UserEditModel userModel,
                         @Parameter(description = "users.field.username") @PathVariable("username") String username) {
        checkUserExists(username);
        validationProvider.validate(userModel);
        validateGroupsNotProvided(userModel.getGroups());
        var dbUser = userManagementService.getUser(username);
        var emailChanged = !Objects.equals(dbUser.getEmail(), userModel.getEmail()) && !dbUser.getExternalFlags()
                .isEmailExternal();
        userManagementService.updateUserData(username,
                userModel.getFirstName(),
                userModel.getLastName(),
                userModel.getPassword(),
                userModel.getEmail(),
                userModel.getDisplayName(),
                !emailChanged && dbUser.getExternalFlags().isEmailVerified());
        if (!groupsDisabled) {
            var leaveAdminGroups = adminUsersInitializer.isSuperuser(username) || Objects
                    .equals(currentUserInfo.getUserName(), username);
            userManagementService.updateAuthorities(username, userModel.getGroups(), leaveAdminGroups);
        }

        if (StringUtils.isNotBlank(userModel.getEmail()) && emailChanged) {
            mailSender.sendVerificationMail(userManagementService.getUser(username), request);
        }
    }

    @Operation(description = "users.edit-user-info.desc", summary = "users.edit-user-info.summary")
    @PutMapping("/info")
    public void editUserInfo(HttpServletRequest request, @RequestBody UserInfoEditModel userModel) {
        validationProvider.validate(userModel);
        var dbUser = userManagementService.getUser(currentUserInfo.getUserName());
        if (updateCurrentUserData(dbUser, userModel, null)) {
            sendVerificationMail(request);
        }
    }

    @Operation(description = "users.edit-user-profile.desc", summary = "users.edit-user-profile.summary")
    @PutMapping("/profile")
    public void editUserProfile(HttpServletRequest request, @RequestBody UserProfileEditModel userModel) {
        validationProvider.validate(userModel);
        var newPassword = Optional.ofNullable(userModel.getChangePassword())
                .map(ChangePasswordModel::getNewPassword)
                .orElse(null);
        // The details and the settings are saved together, so a failure leaves the profile as it was, and the
        // verification link is mailed only once the new e-mail is saved.
        var verifyEmail = txTemplate.execute(status -> {
            var username = currentUserInfo.getUserName();
            var dbUser = userManagementService.getUser(username);
            var emailChanged = updateCurrentUserData(dbUser, withStoredDetails(userModel, dbUser), newPassword);
            userModel.store((key, value) -> userSettingsManager.setProperty(username, key, value));
            return emailChanged;
        });
        if (Boolean.TRUE.equals(verifyEmail)) {
            sendVerificationMail(request);
        }
    }

    /**
     * Saves the details of the current user.
     *
     * @return whether the e-mail has changed and has to be verified
     */
    private boolean updateCurrentUserData(User dbUser, UserInfoModel userModel, @Nullable String newPassword) {
        var emailChanged = !Objects.equals(dbUser.getEmail(), userModel.getEmail()) && !dbUser.getExternalFlags()
                .isEmailExternal();
        userManagementService.updateUserData(dbUser.getUsername(),
                userModel.getFirstName(),
                userModel.getLastName(),
                newPassword,
                userModel.getEmail(),
                userModel.getDisplayName(),
                !emailChanged && dbUser.getExternalFlags().isEmailVerified());
        return emailChanged && StringUtils.isNotBlank(userModel.getEmail());
    }

    private void sendVerificationMail(HttpServletRequest request) {
        mailSender.sendVerificationMail(userManagementService.getUser(currentUserInfo.getUserName()), request);
    }

    /**
     * The details that the request sends, with the stored ones in place of those it leaves out.
     */
    private static UserInfoModel withStoredDetails(UserInfoModel sent, User stored) {
        return new UserInfoModel().setEmail(ObjectUtils.firstNonNull(sent.getEmail(), stored.getEmail()))
                .setFirstName(ObjectUtils.firstNonNull(sent.getFirstName(), stored.getFirstName()))
                .setLastName(ObjectUtils.firstNonNull(sent.getLastName(), stored.getLastName()))
                .setDisplayName(ObjectUtils.firstNonNull(sent.getDisplayName(), stored.getDisplayName()));
    }

    @Operation(description = "users.get-user-profile.desc", summary = "users.get-user-profile.summary")
    @GetMapping("/profile")
    public UserProfileModel getUserProfile() {
        var username = currentUserInfo.getUserName();
        var user = userManagementService.getUser(username);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var isAdmin = SecurityUtils.hasAuthority(authentication, Privileges.ADMIN.getAuthority());

        var profile = new UserProfileModel().setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setEmail(user.getEmail())
                .setDisplayName(user.getDisplayName())
                .setUsername(user.getUsername())
                .setExternalFlags(user.getExternalFlags())
                .setAdministrator(isAdmin);
        profile.load(userSettingsManager.getSettings(user.getUsername()));
        return profile;
    }

    @Operation(description = "users.delete-user.desc", summary = "users.delete-user.summary")
    @DeleteMapping("/{username}")
    @AdminPrivilege
    public void deleteUser(@Parameter(description = "users.field.username") @PathVariable("username") String username) {
        checkUserExists(username);
        checkCanDeleteUser(username);
        userManagementService.deleteUser(username);
        if (aclService != null) {
            aclService.deleteSid(new PrincipalSid(username));
        }
    }

    private void checkCanDeleteUser(String username) {
        if (adminUsersInitializer.isSuperuser(username)) {
            throw new ForbiddenException("users.cannot-delete-superuser.message");
        }
        if (Objects.equals(currentUserInfo.getUserName(), username)) {
            throw new ForbiddenException("users.cannot-delete-yourself.message");
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
        return extGroups.stream().map(Group::getAuthority).collect(StreamUtils.toTreeSet(String.CASE_INSENSITIVE_ORDER));
    }

    private UserModel mapUser(User user) {
        var userModel = new UserModel().setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setEmail(user.getEmail())
                .setUsername(user.getUsername())
                .setCurrentUser(Objects.equals(currentUserInfo.getUserName(), user.getUsername()))
                .setSuperUser(adminUsersInitializer.isSuperuser(user.getUsername()))
                .setUnsafePassword(
                        user.getPassword() != null && passwordEncoder.matches(user.getUsername(), user.getPassword()))
                .setDisplayName(user.getDisplayName())
                .setOnline(userManagementService.isUserOnline(user.getUsername()))
                .setLastLoginTime(user.getLastLoginTime())
                .setExternalFlags(user.getExternalFlags());

        if (!groupsDisabled) {
            var extGroups = extGroupService.findMatchedForUser(user.getUsername());
            var matchedExtGroupsStream = extGroups.stream()
                    .map(simpleGroup -> new GroupModel().setName(simpleGroup.getAuthority())
                            .setType(simpleGroup.hasPrivilege(Privileges.ADMIN.name()) ? GroupType.ADMIN
                                    : GroupType.EXTERNAL));
            var internalGroupStream = user.getAuthorities()
                    .stream()
                    .map(SimpleGroup.class::cast)
                    .filter(g -> extGroups.stream()
                            .noneMatch(ext -> Objects.equals(ext.getAuthority(), g.getAuthority())))
                    .map(simpleGroup -> new GroupModel().setName(simpleGroup.getAuthority())
                            .setType(simpleGroup.hasPrivilege(Privileges.ADMIN.name()) ? GroupType.ADMIN
                                    : GroupType.DEFAULT));
            userModel.setUserGroups(Stream.concat(matchedExtGroupsStream, internalGroupStream)
                    .collect(StreamUtils.toTreeSet(Comparator.comparing(GroupModel::getType)
                            .thenComparing(GroupModel::getName, String.CASE_INSENSITIVE_ORDER))));
            userModel.setNotMatchedExternalGroupsCount(extGroupService.countNotMatchedForUser(user.getUsername()));
        }

        return userModel;
    }

    private void checkUserExists(String username) {
        if (!userManagementService.existsByName(username)) {
            throw new NotFoundException("users.message", username);
        }
    }

}
