package org.openl.rules.rest;

import static org.openl.rules.ui.WebStudio.TABLE_FORMULAS_SHOW;
import static org.openl.rules.ui.WebStudio.TABLE_VIEW;
import static org.openl.rules.ui.WebStudio.TEST_FAILURES_ONLY;
import static org.openl.rules.ui.WebStudio.TEST_FAILURES_PERTEST;
import static org.openl.rules.ui.WebStudio.TEST_RESULT_COMPLEX_SHOW;
import static org.openl.rules.ui.WebStudio.TEST_TESTS_PERPAGE;
import static org.openl.rules.ui.WebStudio.TRACE_REALNUMBERS_SHOW;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.rest.model.GroupModel;
import org.openl.rules.rest.model.GroupType;
import org.openl.rules.rest.model.UserCreateModel;
import org.openl.rules.rest.model.UserEditModel;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.rest.model.UserModel;
import org.openl.rules.rest.model.UserProfileEditModel;
import org.openl.rules.rest.model.UserProfileModel;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.security.UserExternalFlags.Feature;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.PrivilegesEvaluator;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StreamUtils;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UsersService {

    private final UserManagementService userManagementService;
    private final Boolean canCreateInternalUsers;
    private final Boolean canCreateExternalUsers;
    private final AdminUsers adminUsersInitializer;
    private final CurrentUserInfo currentUserInfo;
    private final BeanValidationProvider validationProvider;
    private final UserSettingManagementService userSettingsManager;
    private final PropertyResolver environment;
    private final PasswordEncoder passwordEncoder;
    private final ExternalGroupService extGroupService;

    @Inject
    public UsersService(UserManagementService userManagementService,
            Boolean canCreateInternalUsers,
            Boolean canCreateExternalUsers,
            AdminUsers adminUsersInitializer,
            CurrentUserInfo currentUserInfo,
            PasswordEncoder passwordEncoder,
            PropertyResolver environment,
            BeanValidationProvider validationService,
            UserSettingManagementService userSettingsManager,
            ExternalGroupService extGroupService) {
        this.userManagementService = userManagementService;
        this.canCreateInternalUsers = canCreateInternalUsers;
        this.canCreateExternalUsers = canCreateExternalUsers;
        this.adminUsersInitializer = adminUsersInitializer;
        this.currentUserInfo = currentUserInfo;
        this.passwordEncoder = passwordEncoder;
        this.userSettingsManager = userSettingsManager;
        this.environment = environment;
        this.validationProvider = validationService;
        this.extGroupService = extGroupService;
    }

    @GET
    public List<UserModel> getAllUsers() {
        return userManagementService.getAllUsers().stream().map(this::mapUser).collect(Collectors.toList());
    }

    @GET
    @Path("/{username}")
    public UserModel getUser(@PathParam("username") String username) {
        if (!currentUserInfo.getUserName().equals(username)) {
            SecurityChecker.allow(Privileges.ADMIN);
        }
        checkUserExists(username);
        return Optional.ofNullable(userManagementService.getUser(username)).map(this::mapUser).orElse(null);
    }

    @PUT
    public void addUser(UserCreateModel userModel) {
        SecurityChecker.allow(Privileges.ADMIN);
        validationProvider.validate(userModel);
        boolean willBeExternalUser = canCreateExternalUsers && (!userModel.getInternalPassword()
            .isInternalUser() || !canCreateInternalUsers);
        userManagementService.addUser(userModel.getUsername(),
            userModel.getFirstName(),
            userModel.getLastName(),
            willBeExternalUser ? null : passwordEncoder.encode(userModel.getInternalPassword().getPassword()),
            userModel.getEmail(),
            userModel.getDisplayName(),
            UserExternalFlags.builder().build());
        userManagementService.updateAuthorities(userModel.getUsername(), userModel.getGroups());
    }

    @PUT
    @Path("/{username}")
    public void editUser(@RequestBody UserEditModel userModel, @PathParam("username") String username) {
        if (!currentUserInfo.getUserName().equals(username)) {
            SecurityChecker.allow(Privileges.ADMIN);
        }
        checkUserExists(username);
        validationProvider.validate(userModel);
        boolean updatePassword = Optional.ofNullable(userModel.getPassword())
            .map(StringUtils::isNotBlank)
            .orElse(false);
        userManagementService.updateUserData(username,
            userModel.getFirstName(),
            userModel.getLastName(),
            updatePassword ? passwordEncoder.encode(userModel.getPassword()) : null,
            updatePassword,
            userModel.getEmail(),
            userModel.getDisplayName());
        boolean leaveAdminGroups = adminUsersInitializer.isSuperuser(username) || Objects
            .equals(currentUserInfo.getUserName(), username);
        userManagementService.updateAuthorities(username, userModel.getGroups(), leaveAdminGroups);

        if (currentUserInfo.getUserName().equals(username)) {
            updateCurrentApplicationUser(userModel.getFirstName(),
                userModel.getLastName(),
                userModel.getEmail(),
                userModel.getDisplayName(),
                userModel.getPassword());
        }
    }

    @PUT
    @Path("/info")
    public void editUserInfo(@RequestBody UserInfoModel userModel) {
        validationProvider.validate(userModel);
        userManagementService.updateUserData(currentUserInfo.getUserName(),
            userModel.getFirstName(),
            userModel.getLastName(),
            null,
            false,
            userModel.getEmail(),
            userModel.getDisplayName());

        updateCurrentApplicationUser(userModel
            .getFirstName(), userModel.getLastName(), userModel.getEmail(), userModel.getDisplayName(), null);

    }

    @PUT
    @Path("/profile")
    public void editUserProfile(@RequestBody UserProfileEditModel userModel) {
        validationProvider.validate(userModel);
        boolean updatePassword = Optional.ofNullable(userModel.getChangePassword())
            .map(ChangePasswordModel::getNewPassword)
            .map(StringUtils::isNotBlank)
            .orElse(false);
        userManagementService.updateUserData(currentUserInfo.getUserName(),
            userModel.getFirstName(),
            userModel.getLastName(),
            updatePassword ? passwordEncoder.encode(userModel.getChangePassword().getNewPassword()) : null,
            updatePassword,
            userModel.getEmail(),
            userModel.getDisplayName());

        updateUserSettings(userModel.isShowFormulas(),
            userModel.isShowHeader(),
            userModel.isShowRealNumbers(),
            userModel.getTestsFailuresPerTest(),
            userModel.isShowComplexResult(),
            userModel.getTestsPerPage(),
            userModel.isTestsFailuresOnly());

        updateCurrentApplicationUser(userModel.getFirstName(),
            userModel.getLastName(),
            userModel.getEmail(),
            userModel.getDisplayName(),
            userModel.getChangePassword().getNewPassword());
    }

    private void updateUserSettings(boolean showFormulas,
            boolean showHeader,
            boolean showRealNumbers,
            int testsFailuresPerTest,
            boolean showComplexResult,
            int testsPerPage,
            boolean testsFailuresOnly) {
        WebStudio studio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        String username = currentUserInfo.getUserName();
        if (studio != null) {
            studio.setShowFormulas(showFormulas);
            studio.setShowHeader(showHeader);
            studio.setShowRealNumbers(showRealNumbers);
            studio.setTestsFailuresPerTest(testsFailuresPerTest);
            studio.setShowComplexResult(showComplexResult);
            studio.setTestsPerPage(testsPerPage);
            studio.setTestsFailuresOnly(testsFailuresOnly);
        } else {
            userSettingsManager.setProperty(username, TRACE_REALNUMBERS_SHOW, showRealNumbers);
            userSettingsManager.setProperty(username, TEST_FAILURES_PERTEST, testsFailuresPerTest);
            userSettingsManager.setProperty(username, TEST_RESULT_COMPLEX_SHOW, showComplexResult);
            userSettingsManager.setProperty(username, TEST_TESTS_PERPAGE, testsPerPage);
            userSettingsManager.setProperty(username, TABLE_FORMULAS_SHOW, showFormulas);
            userSettingsManager.setProperty(username,
                TABLE_VIEW,
                showHeader ? IXlsTableNames.VIEW_DEVELOPER : IXlsTableNames.VIEW_BUSINESS);
            userSettingsManager.setProperty(username, TEST_FAILURES_ONLY, testsFailuresOnly);
        }
    }

    private void updateCurrentApplicationUser(String firstname,
            String lastname,
            String email,
            String displayName,
            String newPassword) {
        Optional.ofNullable(currentUserInfo.getAuthentication()).map(authentication -> {
            if (authentication.getPrincipal() instanceof SimpleUser) {
                return (SimpleUser) authentication.getPrincipal();
            } else if (authentication.getDetails() instanceof SimpleUser) {
                return (SimpleUser) authentication.getDetails();
            } else {
                return null;
            }
        }).ifPresent(simpleUser -> {
            simpleUser.setFirstName(firstname);
            simpleUser.setLastName(lastname);
            simpleUser.setEmail(email);
            simpleUser.setDisplayName(displayName);
            if (StringUtils.isNotEmpty(newPassword)) {
                simpleUser.setPassword(passwordEncoder.encode(newPassword));
            }
        });
    }

    @GET
    @Path("/profile")
    public UserProfileModel getUserProfile() {
        String username = currentUserInfo.getUserName();
        User user = Optional.ofNullable(currentUserInfo.getAuthentication()).map(authentication -> {
            if (authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            } else if (authentication.getDetails() instanceof User) {
                return (User) authentication.getDetails();
            } else {
                return userManagementService.getApplicationUser(username);
            }
        }).orElse(new SimpleUser(username, Collections.emptyList()));

        return new UserProfileModel().setFirstName(user.getFirstName())
            .setLastName(user.getLastName())
            .setEmail(user.getEmail())
            .setShowHeader(IXlsTableNames.VIEW_DEVELOPER
                .equals(userSettingsManager.getStringProperty(user.getUsername(), TABLE_VIEW)))
            .setShowFormulas(userSettingsManager.getBooleanProperty(user.getUsername(), TABLE_FORMULAS_SHOW))
            .setTestsPerPage(userSettingsManager.getIntegerProperty(user.getUsername(), TEST_TESTS_PERPAGE))
            .setTestsFailuresOnly(userSettingsManager.getBooleanProperty(user.getUsername(), TEST_FAILURES_ONLY))
            .setTestsFailuresPerTest(userSettingsManager.getIntegerProperty(user.getUsername(), TEST_FAILURES_PERTEST))
            .setShowComplexResult(userSettingsManager.getBooleanProperty(user.getUsername(), TEST_RESULT_COMPLEX_SHOW))
            .setShowRealNumbers(userSettingsManager.getBooleanProperty(user.getUsername(), TRACE_REALNUMBERS_SHOW))
            .setDisplayName(user.getDisplayName())
            .setUsername(user.getUsername())
            .setInternalUser(StringUtils.isNotBlank(user.getPassword()))
            .setExternalFlags(user.getExternalFlags());
    }

    @DELETE
    @Path("/{username}")
    public void deleteUser(@PathParam("username") String username) {
        checkUserExists(username);
        userManagementService.deleteUser(username);
    }

    @GET
    @Path("/options")
    public Map<String, Object> options() {
        HashMap<String, Object> options = new HashMap<>();
        options.put("canCreateInternalUsers", canCreateInternalUsers);
        options.put("canCreateExternalUsers", canCreateExternalUsers);
        options.put("userMode", environment.getProperty("user.mode"));
        return options;
    }

    @GET
    @Path("/{username}/groups/external")
    public Set<String> getUserGroupsGroups(@PathParam("username") String username,
            @QueryParam("matched") Boolean matched) {
        SecurityChecker.allow(Privileges.ADMIN);
        checkUserExists(username);
        List<Group> extGroups;
        if (matched == null) {
            extGroups = extGroupService.findAllForUser(username);
        } else if (Boolean.FALSE.equals(matched)) {
            extGroups = extGroupService.findNotMatchedForUser(username);
        } else {
            extGroups = extGroupService.findMatchedForUser(username);
        }
        return extGroups.stream().map(Group::getName).collect(StreamUtils.toTreeSet(String.CASE_INSENSITIVE_ORDER));
    }

    private UserModel mapUser(org.openl.rules.security.standalone.persistence.User user) {
        List<Group> matchedExtGroups = extGroupService.findMatchedForUser(user.getLoginName());
        long cntNotMatchedExtGroups = extGroupService.countNotMatchedForUser(user.getLoginName());
        return new UserModel().setFirstName(user.getFirstName())
            .setLastName(user.getSurname())
            .setEmail(user.getEmail())
            .setInternalUser(StringUtils.isNotBlank(user.getPasswordHash()))
            .setUserGroups(
                user.getGroups()
                    .stream()
                    .map(PrivilegesEvaluator::wrap)
                    .map(simpleGroup -> new GroupModel().setName(simpleGroup.getName())
                        .setType(simpleGroup.getPrivileges().contains(Privileges.ADMIN) ? GroupType.ADMIN
                                                                                        : GroupType.DEFAULT))
                    .collect(Collectors.toSet()))
            .setUsername(user.getLoginName())
            .setCurrentUser(currentUserInfo.getUserName().equals(user.getLoginName()))
            .setSuperUser(adminUsersInitializer.isSuperuser(user.getLoginName()))
            .setUnsafePassword(
                user.getPasswordHash() != null && passwordEncoder.matches(user.getLoginName(), user.getPasswordHash()))
            .setExternalGroups(matchedExtGroups.stream()
                .map(Group::getName)
                .collect(StreamUtils.toTreeSet(String.CASE_INSENSITIVE_ORDER)))
            .setNotMatchedExternalGroupsCount(cntNotMatchedExtGroups)
            .setDisplayName(user.getDisplayName())
            .setExternalFlags(UserExternalFlags.builder()
                .applyFeature(Feature.EXTERNAL_FIRST_NAME, user.isFirstNameExternal())
                .applyFeature(Feature.EXTERNAL_LAST_NAME, user.isLastNameExternal())
                .applyFeature(Feature.EXTERNAL_EMAIL, user.isEmailExternal())
                .applyFeature(Feature.EXTERNAL_DISPLAY_NAME, user.isDisplayNameExternal())
                .build());
    }

    private void checkUserExists(String username) {
        if (!userManagementService.existsByName(username)) {
            throw new NotFoundException("users.message", username);
        }
    }

}
