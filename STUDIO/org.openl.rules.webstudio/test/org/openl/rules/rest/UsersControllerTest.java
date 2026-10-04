package org.openl.rules.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.request.RequestContextHolder;

import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.rest.model.UserInfoEditModel;
import org.openl.rules.rest.model.UserProfileEditModel;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.service.UserSettings;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.security.CurrentUserInfo;

/**
 * Unit tests for user data editing in {@link UsersController}.
 *
 * @author Yury Molchan
 */
@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private UserManagementService userManagementService;
    @Mock
    private AdminUsers adminUsers;
    @Mock
    private CurrentUserInfo currentUserInfo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BeanValidationProvider validationProvider;
    @Mock
    private UserSettingManagementService userSettingsManager;
    @Mock
    private ExternalGroupService extGroupService;
    @Mock
    private MailSender mailSender;
    @Mock
    private PlatformTransactionManager txManager;
    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
    }

    private UsersController createController(String userMode) {
        return new UsersController(userManagementService,
                Boolean.FALSE,
                adminUsers,
                currentUserInfo,
                passwordEncoder,
                validationProvider,
                userSettingsManager,
                extGroupService,
                mailSender,
                txManager,
                null,
                userMode);
    }

    private SimpleUser dbUser(UserExternalFlags flags) {
        return SimpleUser.builder()
                .setUsername("jdoe")
                .setFirstName("John")
                .setLastName("Doe")
                .setDisplayName("John Doe")
                .setEmail("old@example.com")
                .setExternalFlags(flags)
                .build();
    }

    @Test
    void editUserProfile_updatesUserWithNewPassword() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder().build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var controller = createController("single");

        var model = new UserProfileEditModel();
        model.setChangePassword(new ChangePasswordModel().setNewPassword("secret"));
        model.setFirstName("John").setLastName("Doe").setEmail("old@example.com").setDisplayName("John Doe");

        controller.editUserProfile(request, model);

        verify(userManagementService)
                .updateUserData("jdoe", "John", "Doe", "secret", "old@example.com", "John Doe", false);
    }

    @Test
    void editUserProfile_keepsSettingsLeftOutOfTheRequest() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder().build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var controller = createController("multi");

        var model = new UserProfileEditModel().setChangePassword(new ChangePasswordModel());
        model.setFirstName("Ada").setLastName("Admin").setEmail("old@example.com").setDisplayName("Ada Admin");

        controller.editUserProfile(request, model);

        verify(userManagementService)
                .updateUserData("jdoe", "Ada", "Admin", null, "old@example.com", "Ada Admin", false);
        verifyNoInteractions(userSettingsManager);
    }

    @Test
    void editUserProfile_keepsDetailsLeftOutOfTheRequest() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder().withFeature(UserExternalFlags.Feature.EMAIL_VERIFIED).build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var controller = createController("multi");

        controller.editUserProfile(request, new UserProfileEditModel().setShowFormulas(true));

        verify(userManagementService)
                .updateUserData("jdoe", "John", "Doe", null, "old@example.com", "John Doe", true);
        verify(userSettingsManager).setProperty("jdoe", "table.formulas.show", "true");
        verify(mailSender, never()).sendVerificationMail(any(), any());
    }

    @Test
    void editUserProfile_leavesTheProfileAsItWasWhenASettingCannotBeSaved() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder().build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        doThrow(new IllegalStateException("The database is gone")).when(userSettingsManager)
                .setProperty("jdoe", "table.formulas.show", "true");
        var controller = createController("multi");

        var model = new UserProfileEditModel().setShowFormulas(true);
        model.setEmail("new@example.com");

        assertThrows(IllegalStateException.class, () -> controller.editUserProfile(request, model));

        verify(txManager).rollback(any());
        verify(txManager, never()).commit(any());
        verify(mailSender, never()).sendVerificationMail(any(), any());
    }

    @Test
    void editUserProfile_mailsTheVerificationLinkOnceTheProfileIsSaved() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder().build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var controller = createController("multi");

        var model = new UserProfileEditModel();
        model.setEmail("new@example.com");

        controller.editUserProfile(request, model);

        var inOrder = inOrder(userManagementService, txManager, mailSender);
        inOrder.verify(userManagementService)
                .updateUserData("jdoe", "John", "Doe", null, "new@example.com", "John Doe", false);
        inOrder.verify(txManager).commit(any());
        inOrder.verify(mailSender).sendVerificationMail(dbUser, request);
    }

    @ParameterizedTest
    @CsvSource({"true, developer", "false, business"})
    void editUserProfile_savesTheSettingsOfTheRequest(boolean showHeader, String tableView) {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder().build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var controller = createController("multi");

        var model = new UserProfileEditModel().setShowHeader(showHeader)
                .setShowFormulas(true)
                .setTestsPerPage(20)
                .setTestsFailuresOnly(true)
                .setTestsFailuresPerTest(-1)
                .setShowComplexResult(false)
                .setShowRealNumbers(true);
        model.setEmail("old@example.com").setDisplayName("John Doe");

        controller.editUserProfile(request, model);

        verify(userSettingsManager).setProperty("jdoe", "table.view", tableView);
        verify(userSettingsManager).setProperty("jdoe", "table.formulas.show", "true");
        verify(userSettingsManager).setProperty("jdoe", "test.tests.perpage", "20");
        verify(userSettingsManager).setProperty("jdoe", "test.failures.only", "true");
        verify(userSettingsManager).setProperty("jdoe", "test.failures.pertest", "-1");
        verify(userSettingsManager).setProperty("jdoe", "test.result.complex.show", "false");
        verify(userSettingsManager).setProperty("jdoe", "trace.realNumbers.show", "true");
        verifyNoMoreInteractions(userSettingsManager);
    }

    @Test
    void getUserProfile_readsTheStoredSettings() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder().build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var stored = Map.of("table.view",
                "business",
                "table.formulas.show",
                "true",
                "test.tests.perpage",
                "20",
                "test.failures.only",
                "true",
                "test.failures.pertest",
                "-1",
                "trace.realNumbers.show",
                "true");
        var defaults = new MockEnvironment().withProperty("test.result.complex.show", "false");
        when(userSettingsManager.getSettings("jdoe")).thenReturn(new UserSettings(stored, defaults));
        var controller = createController("multi");

        var profile = controller.getUserProfile();

        assertEquals("jdoe", profile.getUsername());
        assertEquals(false, profile.getShowHeader());
        assertEquals(true, profile.getShowFormulas());
        assertEquals(Integer.valueOf(20), profile.getTestsPerPage());
        assertEquals(true, profile.getTestsFailuresOnly());
        assertEquals(Integer.valueOf(-1), profile.getTestsFailuresPerTest());
        assertEquals(false, profile.getShowComplexResult());
        assertEquals(true, profile.getShowRealNumbers());
    }

    @Test
    void getUserProfile_leavesOutASettingWithNeitherValueNorDefault() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder().build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        when(userSettingsManager.getSettings("jdoe")).thenReturn(new UserSettings(Map.of(), new MockEnvironment()));
        var controller = createController("multi");

        var profile = controller.getUserProfile();

        assertEquals("jdoe", profile.getUsername());
        assertNull(profile.getShowFormulas());
        assertNull(profile.getTestsPerPage());
        assertNull(profile.getTestsFailuresPerTest());
    }

    @Test
    void editUserInfo_updatesUserAndSendsVerificationMail() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder()
                .withFeature(UserExternalFlags.Feature.EMAIL_VERIFIED)
                .build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var controller = createController("multi");

        var model = new UserInfoEditModel();
        model.setFirstName("John").setLastName("Doe").setEmail("new@example.com").setDisplayName("John Doe");

        controller.editUserInfo(request, model);

        verify(userManagementService)
                .updateUserData("jdoe", "John", "Doe", null, "new@example.com", "John Doe", false);
        verify(mailSender).sendVerificationMail(dbUser, request);
    }

    @Test
    void editUserInfo_keepsVerifiedEmailWhenUnchanged() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder()
                .withFeature(UserExternalFlags.Feature.EMAIL_VERIFIED)
                .build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var controller = createController("multi");

        var model = new UserInfoEditModel();
        model.setFirstName("John").setLastName("Doe").setEmail("old@example.com").setDisplayName("John Doe");

        controller.editUserInfo(request, model);

        verify(userManagementService)
                .updateUserData("jdoe", "John", "Doe", null, "old@example.com", "John Doe", true);
        verify(mailSender, never()).sendVerificationMail(any(), any());
    }

    @Test
    void getAllUsers_returnsLastLoginTime() {
        when(currentUserInfo.getUserName()).thenReturn("admin");
        var lastLoginTime = Instant.parse("2026-07-08T10:15:30Z");
        var user = SimpleUser.builder()
                .setUsername("jdoe")
                .setLastLoginTime(lastLoginTime)
                .build();
        when(userManagementService.getAllUsers()).thenReturn(List.of(user));
        var controller = createController("multi");

        var users = controller.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("jdoe", users.getFirst().getUsername());
        assertEquals(lastLoginTime, users.getFirst().getLastLoginTime());
        assertNull(users.getFirst().getUserGroups());
    }
}
