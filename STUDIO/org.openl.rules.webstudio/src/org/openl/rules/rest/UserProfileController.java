package org.openl.rules.rest;

import static org.openl.rules.ui.WebStudio.RULES_TREE_VIEW_DEFAULT;
import static org.openl.rules.ui.WebStudio.TABLE_FORMULAS_SHOW;
import static org.openl.rules.ui.WebStudio.TABLE_VIEW;
import static org.openl.rules.ui.WebStudio.TEST_FAILURES_ONLY;
import static org.openl.rules.ui.WebStudio.TEST_FAILURES_PERTEST;
import static org.openl.rules.ui.WebStudio.TEST_RESULT_COMPLEX_SHOW;
import static org.openl.rules.ui.WebStudio.TEST_TESTS_PERPAGE;
import static org.openl.rules.ui.WebStudio.TRACE_REALNUMBERS_SHOW;

import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.rest.model.UserProfileEditModel;
import org.openl.rules.rest.model.UserProfileModel;
import org.openl.rules.security.AccessManager;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.User;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.view.Profile;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users")
@Validated
public class UserProfileController {

    private final UserManagementService userManagementService;
    private final UserSettingManagementService userSettingsManager;
    private final CurrentUserInfo currentUserInfo;
    private final MailSender mailSender;
    private final PropertyResolver environment;
    private final Boolean canCreateInternalUsers;

    public UserProfileController(UserManagementService userManagementService,
                                 UserSettingManagementService userSettingsManager,
                                 CurrentUserInfo currentUserInfo,
                                 MailSender mailSender,
                                 PropertyResolver environment,
                                 Boolean canCreateInternalUsers) {
        this.userManagementService = userManagementService;
        this.userSettingsManager = userSettingsManager;
        this.currentUserInfo = currentUserInfo;
        this.mailSender = mailSender;
        this.environment = environment;
        this.canCreateInternalUsers = canCreateInternalUsers;
    }

    @Operation(description = "users.edit-user-info.desc", summary = "users.edit-user-info.summary")
    @PutMapping("/info")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editUserInfo(HttpServletRequest request, @Valid @RequestBody UserInfoModel userModel) {
        User dbUser = userManagementService.getUser(currentUserInfo.getUserName());
        boolean emailChanged = !Objects.equals(dbUser.getEmail(), userModel.getEmail()) && !dbUser.getExternalFlags()
                .isEmailExternal();
        userManagementService.updateUserData(currentUserInfo.getUserName(),
                userModel.getFirstName(),
                userModel.getLastName(),
                null,
                userModel.getEmail(),
                userModel.getDisplayName(),
                !emailChanged && dbUser.getExternalFlags().isEmailVerified());

        if (StringUtils.isNotBlank(userModel.getEmail()) && emailChanged) {
            mailSender.sendVerificationMail(userManagementService.getUser(currentUserInfo.getUserName()), request);
        }
    }

    @Operation(description = "users.edit-user-profile.desc", summary = "users.edit-user-profile.summary")
    @PutMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editUserProfile(HttpServletRequest request, @Valid @RequestBody UserProfileEditModel userModel) {
        User dbUser = userManagementService.getUser(currentUserInfo.getUserName());
        boolean emailChanged = !Objects.equals(dbUser.getEmail(), userModel.getEmail()) && !dbUser.getExternalFlags()
                .isEmailExternal();
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
                userModel.isTestsFailuresOnly(),
                userModel.getTreeView());

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
                                    boolean testsFailuresOnly,
                                    String treeView) {
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
            studio.setDefaultTreeView(treeView);
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
            userSettingsManager.setProperty(username, RULES_TREE_VIEW_DEFAULT, treeView);
        }
    }

    @Operation(description = "users.get-user-profile.desc", summary = "users.get-user-profile.summary")
    @GetMapping("/profile")
    public UserProfileModel getUserProfile() {
        String username = currentUserInfo.getUserName();
        User user = userManagementService.getUser(username);

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
                .setTreeView(userSettingsManager.getStringProperty(user.getUsername(), RULES_TREE_VIEW_DEFAULT))
                .setDisplayName(user.getDisplayName())
                .setUsername(user.getUsername())
                .setExternalFlags(user.getExternalFlags())
                .setAdministrator(AccessManager.isGranted(Privileges.ADMIN))
                .setProfiles(Profile.PROFILES);

    }

    @Deprecated
    @Operation(description = "users.options.desc", summary = "users.options.summary")
    @GetMapping("/options")
    public UserOptions options() {
        return UserOptions.builder()
                .canCreateInternalUsers(canCreateInternalUsers)
                .userMode(environment.getProperty("user.mode"))
                .emailVerification(mailSender.isValidEmailSettings())
                .build();
    }

    @Deprecated
    public static class UserOptions {

        @Parameter(description = "Can create internal users")
        public final Boolean canCreateInternalUsers;

        @Parameter(description = "User mode")
        public final String userMode;

        @Parameter(description = "Is e-mail verification required")
        public final Boolean emailVerification;

        public UserOptions(Builder from) {
            this.canCreateInternalUsers = from.canCreateInternalUsers;
            this.userMode = from.userMode;
            this.emailVerification = from.emailVerification;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private Boolean canCreateInternalUsers;
            private String userMode;
            private Boolean emailVerification;

            private Builder() {
            }

            public Builder canCreateInternalUsers(Boolean canCreateInternalUsers) {
                this.canCreateInternalUsers = canCreateInternalUsers;
                return this;
            }

            public Builder userMode(String userMode) {
                this.userMode = userMode;
                return this;
            }

            public Builder emailVerification(Boolean emailVerification) {
                this.emailVerification = emailVerification;
                return this;
            }

            public UserOptions build() {
                return new UserOptions(this);
            }

        }

    }


}
