package org.openl.rules.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;

import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.rest.model.UserProfileEditModel;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
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

    private final HttpServletRequest request = mock(HttpServletRequest.class);

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
                new MockEnvironment(),
                validationProvider,
                userSettingsManager,
                extGroupService,
                mailSender,
                null,
                userMode);
    }

    private SimpleUser dbUser(UserExternalFlags flags) {
        return SimpleUser.builder()
                .setUsername("jdoe")
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
    void editUserInfo_updatesUserAndSendsVerificationMail() {
        when(currentUserInfo.getUserName()).thenReturn("jdoe");
        var dbUser = dbUser(UserExternalFlags.builder()
                .withFeature(UserExternalFlags.Feature.EMAIL_VERIFIED)
                .build());
        when(userManagementService.getUser("jdoe")).thenReturn(dbUser);
        var controller = createController("multi");

        var model = new UserInfoModel();
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

        var model = new UserInfoModel();
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
