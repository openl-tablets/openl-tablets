package org.openl.rules.rest;

import static org.openl.rules.ui.WebStudio.TABLE_FORMULAS_SHOW;
import static org.openl.rules.ui.WebStudio.TABLE_VIEW;
import static org.openl.rules.ui.WebStudio.TEST_FAILURES_ONLY;
import static org.openl.rules.ui.WebStudio.TEST_FAILURES_PERTEST;
import static org.openl.rules.ui.WebStudio.TEST_RESULT_COMPLEX_SHOW;
import static org.openl.rules.ui.WebStudio.TEST_TESTS_PERPAGE;
import static org.openl.rules.ui.WebStudio.TRACE_REALNUMBERS_SHOW;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

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
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StreamUtils;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsersController {

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
    private final MailSender mailSender;

    @Autowired
    public UsersController(UserManagementService userManagementService,
            Boolean canCreateInternalUsers,
            Boolean canCreateExternalUsers,
            AdminUsers adminUsersInitializer,
            CurrentUserInfo currentUserInfo,
            PasswordEncoder passwordEncoder,
            PropertyResolver environment,
            BeanValidationProvider validationService,
            UserSettingManagementService userSettingsManager,
            ExternalGroupService extGroupService,
            MailSender mailSender) {
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
        this.mailSender = mailSender;
    }

    @GetMapping
    public List<UserModel> getAllUsers() {
        return userManagementService.getAllUsers().stream().map(this::mapUser).collect(Collectors.toList());
    }

    @GetMapping("/{username}")
    public UserModel getUser(@PathVariable("username") String username) {
        if (!currentUserInfo.getUserName().equals(username)) {
            SecurityChecker.allow(Privileges.ADMIN);
        }
        checkUserExists(username);
        return Optional.ofNullable(userManagementService.getUser(username)).map(this::mapUser).orElse(null);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addUser(HttpServletRequest request, @RequestBody UserCreateModel userModel) {
        SecurityChecker.allow(Privileges.ADMIN);
        validationProvider.validate(userModel);
        userManagementService.addUser(userModel.getUsername(),
            userModel.getFirstName(),
            userModel.getLastName(),
            canCreateInternalUsers ? userModel.getInternalPassword().getPassword() : null,
            userModel.getEmail(),
            userModel.getDisplayName(),
            UserExternalFlags.builder().build());
        userManagementService.updateAuthorities(userModel.getUsername(), userModel.getGroups());
        if (StringUtils.isNotBlank(userModel.getEmail())) {
            mailSender.sendVerificationMail(userManagementService.getUser(userModel.getUsername()), request);
        }
    }

    @PutMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editUser(HttpServletRequest request,
                         @RequestBody UserEditModel userModel,
                         @PathVariable("username") String username) {
        if (!currentUserInfo.getUserName().equals(username)) {
            SecurityChecker.allow(Privileges.ADMIN);
        }
        checkUserExists(username);
        validationProvider.validate(userModel);
        User dbUser = userManagementService.getUser(username);
        boolean emailChanged = !Objects.equals(dbUser.getEmail(), userModel.getEmail())
                && !dbUser.getExternalFlags().isEmailExternal();
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

        if (currentUserInfo.getUserName().equals(username)) {
            updateCurrentApplicationUser(userModel.getFirstName(),
                userModel.getLastName(),
                userModel.getEmail(),
                userModel.getDisplayName(),
                userModel.getPassword(),
                !emailChanged && dbUser.getExternalFlags().isEmailVerified());
        }

        if (StringUtils.isNotBlank(userModel.getEmail()) && emailChanged) {
            mailSender.sendVerificationMail(userManagementService.getUser(username), request);
        }
    }

    @PutMapping("/info")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editUserInfo(HttpServletRequest request, @RequestBody UserInfoModel userModel) {
        validationProvider.validate(userModel);
        User dbUser = userManagementService.getUser(currentUserInfo.getUserName());
        boolean emailChanged = !Objects.equals(dbUser.getEmail(), userModel.getEmail())
                && !dbUser.getExternalFlags().isEmailExternal();
        userManagementService.updateUserData(currentUserInfo.getUserName(),
            userModel.getFirstName(),
            userModel.getLastName(),
            null,
            userModel.getEmail(),
            userModel.getDisplayName(),
            !emailChanged && dbUser.getExternalFlags().isEmailVerified());

        updateCurrentApplicationUser(userModel.getFirstName(),
            userModel.getLastName(),
            userModel.getEmail(),
            userModel.getDisplayName(),
            null,
            !emailChanged && dbUser.getExternalFlags().isEmailVerified());

        if (StringUtils.isNotBlank(userModel.getEmail()) && emailChanged) {
            mailSender.sendVerificationMail(userManagementService.getUser(currentUserInfo.getUserName()), request);
        }
    }

    @PutMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editUserProfile(HttpServletRequest request, @RequestBody UserProfileEditModel userModel) {
        validationProvider.validate(userModel);
        User dbUser = userManagementService.getUser(currentUserInfo.getUserName());
        boolean emailChanged = !Objects.equals(dbUser.getEmail(), userModel.getEmail())
                && !dbUser.getExternalFlags().isEmailExternal();
        userManagementService.updateUserData(dbUser.getUsername(),
            userModel.getFirstName(),
            userModel.getLastName(),
            Optional.ofNullable(userModel.getChangePassword()).map(ChangePasswordModel::getNewPassword).orElse(null),
            userModel.getEmail(),
            userModel.getDisplayName(),
            !emailChanged && dbUser.getExternalFlags().isEmailVerified());

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
            userModel.getChangePassword().getNewPassword(),
            !emailChanged && dbUser.getExternalFlags().isEmailVerified());

        if (StringUtils.isNotBlank(userModel.getEmail()) && emailChanged) {
            mailSender.sendVerificationMail(userManagementService.getUser(currentUserInfo.getUserName()), request);
        }
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
            String newPassword,
            boolean emailVerified) {
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
            simpleUser.setExternalFlags(UserExternalFlags.builder(simpleUser.getExternalFlags())
                .applyFeature(UserExternalFlags.Feature.EMAIL_VERIFIED, emailVerified)
                .build());
            if (StringUtils.isNotEmpty(newPassword)) {
                simpleUser.setPassword(newPassword);
            }
        });
    }

    @GetMapping("/profile")
    public UserProfileModel getUserProfile() {
        String username = currentUserInfo.getUserName();
        User user = Optional.ofNullable(currentUserInfo.getAuthentication()).map(authentication -> {
            if (authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            } else if (authentication.getDetails() instanceof User) {
                return (User) authentication.getDetails();
            } else {
                return userManagementService.getUser(username);
            }
        }).orElse(SimpleUser.builder().setUsername(username).build());

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
            .setExternalFlags(user.getExternalFlags());
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("username") String username) {
        checkUserExists(username);
        userManagementService.deleteUser(username);
    }

    @GetMapping("/options")
    public Map<String, Object> options() {
        HashMap<String, Object> options = new HashMap<>();
        options.put("canCreateInternalUsers", canCreateInternalUsers);
        options.put("canCreateExternalUsers", canCreateExternalUsers);
        options.put("userMode", environment.getProperty("user.mode"));
        options.put("emailVerification", mailSender.isValidEmailSettings());
        return options;
    }

    @GetMapping("/{username}/groups/external")
    public Set<String> getUserGroupsGroups(@PathVariable("username") String username,
            @RequestParam(value = "matched", required = false) Boolean matched) {
        SecurityChecker.allow(Privileges.ADMIN);
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
