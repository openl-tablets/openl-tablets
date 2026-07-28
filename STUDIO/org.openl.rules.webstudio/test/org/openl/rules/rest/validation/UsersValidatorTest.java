package org.openl.rules.rest.validation;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.rest.model.InternalPasswordModel;
import org.openl.rules.rest.model.UserCreateModel;
import org.openl.rules.rest.model.UserEditModel;
import org.openl.rules.rest.model.UserInfoEditModel;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.rest.model.UserProfileEditModel;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.studio.common.validation.AbstractConstraintValidatorTest;
import org.openl.studio.security.CurrentUserInfo;

@SpringJUnitConfig(classes = MockConfiguration.class)
class UsersValidatorTest extends AbstractConstraintValidatorTest {

    private static final String MUST_BE_LESS_THAN_25 = "Must be less than 25.";
    private static final String CANNOT_BE_EMPTY = "Cannot be empty.";
    private static final String MUST_NOT_CONTAIN_FOLLOWING_CHARS = "The name cannot contain spaces and any of the following characters: / \\ : * ? \" < > | { } ~ ^ ; %";

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private CurrentUserInfo currentUserInfo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void reset_mocks() {
        reset(userManagementService, passwordEncoder, currentUserInfo);
    }

    @Test
    void testUserInfo_valid() {
        assertNull(validateAndGetResult(getValidUserInfoModel()));
    }

    @Test
    void testUserInfo_firstName_notValid() {
        var userInfoModel = getValidUserInfoModel();
        String wrongFirstName = RandomStringUtils.random(26, "John");
        userInfoModel.setFirstName(wrongFirstName);
        var bindingResult = validateAndGetResult(userInfoModel);
        assertFieldError("firstName", MUST_BE_LESS_THAN_25, wrongFirstName, bindingResult.getFieldError("firstName"));
    }

    @Test
    void testUserInfo_lastName_notValid() {
        var userInfoModel = getValidUserInfoModel();
        String wrongLastName = RandomStringUtils.random(26, "Smith");
        userInfoModel.setLastName(wrongLastName);
        var bindingResult = validateAndGetResult(userInfoModel);
        assertFieldError("lastName", MUST_BE_LESS_THAN_25, wrongLastName, bindingResult.getFieldError("lastName"));
    }

    @Test
    void testUserInfo_displayName_notValid() {
        var userInfoModel = getValidUserInfoModel();
        String wrongDisplayName = RandomStringUtils.random(65, "John Smith");
        userInfoModel.setDisplayName(wrongDisplayName);
        var bindingResult = validateAndGetResult(userInfoModel);
        assertFieldError("displayName",
                "Must be less than 64.",
                wrongDisplayName,
                bindingResult.getFieldError("displayName"));
    }

    @Test
    void testUserInfo_email_notValid() {
        var userInfoModel = getValidUserInfoModel();
        userInfoModel.setEmail("wrongEmail");
        var bindingResult = validateAndGetResult(userInfoModel);
        assertFieldError("email", "The email address is invalid.", "wrongEmail", bindingResult.getFieldError("email"));
    }

    @Test
    void testEditUserInfo_requiredFields() {
        var userInfoEditModel = getValidUserInfoEditModel();

        userInfoEditModel.setEmail("");
        var bindingResult = validateAndGetResult(userInfoEditModel);
        assertFieldError("email", CANNOT_BE_EMPTY, "", bindingResult.getFieldError("email"));

        userInfoEditModel.setEmail("jsmith@email").setDisplayName("");
        bindingResult = validateAndGetResult(userInfoEditModel);
        assertFieldError("displayName", CANNOT_BE_EMPTY, "", bindingResult.getFieldError("displayName"));
    }

    @Test
    void testEditRequests_firstAndLastNames_optional() {
        when(userManagementService.getUser(anyString())).thenReturn(null);

        assertOptionalNamesValid(getValidUserInfoEditModel());
        assertOptionalNamesValid(getValidUserEditModel());
        assertOptionalNamesValid(getValidUserCreateModel());
        assertOptionalNamesValid(getValidUserProfileEditModel().setChangePassword(new ChangePasswordModel()));
    }

    @Test
    void testEditUser_valid() {
        var userEditModel = getValidUserEditModel();

        assertNull(validateAndGetResult(userEditModel));

        userEditModel.setPassword(null);
        assertNull(validateAndGetResult(userEditModel));
    }

    @Test
    void testEditUser_password_notValid() {
        var userEditModel = getValidUserEditModel();
        String wrongPassword = RandomStringUtils.random(26, "pass");
        userEditModel.setPassword(wrongPassword);
        var bindingResult = validateAndGetResult(userEditModel);
        assertFieldError("password", MUST_BE_LESS_THAN_25, wrongPassword, bindingResult.getFieldError("password"));
    }

    @Test
    void testEditUser_requiredFields() {
        var userEditModel = getValidUserEditModel().setEmail("");
        var bindingResult = validateAndGetResult(userEditModel);
        assertFieldError("email", CANNOT_BE_EMPTY, "", bindingResult.getFieldError("email"));

        userEditModel.setEmail("jsmith@email").setDisplayName(" ");
        bindingResult = validateAndGetResult(userEditModel);
        assertFieldError("displayName", CANNOT_BE_EMPTY, " ", bindingResult.getFieldError("displayName"));
    }

    @Test
    void testCreateUser_valid() {
        when(userManagementService.getUser(anyString())).thenReturn(null);
        var userCreateModel = getValidUserCreateModel();

        assertNull(validateAndGetResult(getValidUserCreateModel()));

        userCreateModel.setInternalPassword(new InternalPasswordModel().setPassword("password"));
        assertNull(validateAndGetResult(userCreateModel));

        userCreateModel.setUsername("a1!@#$&()_-+='.,");
        assertNull(validateAndGetResult(userCreateModel));

        userCreateModel.setUsername("фы漢語,汉语ęął");
        assertNull(validateAndGetResult(userCreateModel));

        userCreateModel.setUsername("a");
        assertNull(validateAndGetResult(userCreateModel));
    }

    @Test
    void testCreateUser_noGroups_valid() {
        var userCreateModel = getValidUserCreateModel();
        userCreateModel.setGroups(null);
        assertNull(validateAndGetResult(userCreateModel));
    }

    @Test
    void testCreateUser_requiredFields() {
        when(userManagementService.getUser(anyString())).thenReturn(null);
        var userCreateModel = getValidUserCreateModel().setEmail("");
        var bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("email", CANNOT_BE_EMPTY, "", bindingResult.getFieldError("email"));

        userCreateModel.setEmail("jsmith@email").setDisplayName("");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("displayName", CANNOT_BE_EMPTY, "", bindingResult.getFieldError("displayName"));
    }

    @Test
    void testCreateUser_password_notValid() {
        when(userManagementService.getUser(anyString())).thenReturn(null);
        var userCreateModel = getValidUserCreateModel();

        var wrongInternalPassword = new InternalPasswordModel().setPassword(null);
        userCreateModel.setInternalPassword(wrongInternalPassword);
        var bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("internalPassword",
                CANNOT_BE_EMPTY,
                wrongInternalPassword,
                bindingResult.getFieldError("internalPassword"));

        wrongInternalPassword = new InternalPasswordModel().setPassword(RandomStringUtils.random(26, "pass"));
        userCreateModel.setInternalPassword(wrongInternalPassword);
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("internalPassword",
                MUST_BE_LESS_THAN_25,
                wrongInternalPassword,
                bindingResult.getFieldError("internalPassword"));
    }

    @Test
    void testCreateUser_username_notValid() {
        when(userManagementService.getUser(anyString())).thenReturn(null);
        var userCreateModel = getValidUserCreateModel();

        String wrongUsername = RandomStringUtils.random(26, "jsmith");
        userCreateModel.setUsername(wrongUsername);
        var bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_BE_LESS_THAN_25, wrongUsername, bindingResult.getFieldError("username"));

        userCreateModel.setUsername(null);
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", CANNOT_BE_EMPTY, null, bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a..aa");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", "The name cannot contain consecutive '.'.", "a..aa", bindingResult.getFieldError("username"));

        userCreateModel.setUsername(".aa");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
                "The name cannot start or end with '.'.",
                ".aa",
                bindingResult.getFieldError("username"));

        userCreateModel.setUsername("aa.");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
                "The name cannot start or end with '.'.",
                "aa.",
                bindingResult.getFieldError("username"));

        userCreateModel.setUsername(" aa");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, " aa", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("aa ");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "aa ", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a/");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a/", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a\\");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a\\", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a:");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a:", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a*");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a*", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a?");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a?", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a\"");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a\"", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a<");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a<", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a|");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a|", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a{");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a{", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a~");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a~", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a^");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a^", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a%");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a%", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a;");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a;", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a\u2028");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
                MUST_NOT_CONTAIN_FOLLOWING_CHARS,
                "a\u2028",
                bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a\u2029");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
                MUST_NOT_CONTAIN_FOLLOWING_CHARS,
                "a\u2029",
                bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a\t");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a\t", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a\r");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a\r", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a\n");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_NOT_CONTAIN_FOLLOWING_CHARS, "a\n", bindingResult.getFieldError("username"));

        userCreateModel.setUsername("jsmith");
        when(userManagementService.existsByName(anyString())).thenReturn(Boolean.TRUE);
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
                "A user with such username already exists.",
                "jsmith",
                bindingResult.getFieldError("username"));

    }

    @Test
    void testEditUserProfile_valid() {
        var userProfileEditModel = getValidUserProfileEditModel();
        when(currentUserInfo.getUserName()).thenReturn("jsmith");
        when(passwordEncoder.matches("pass", "passHash")).thenReturn(true);
        var existedUser = new SimpleUser();
        existedUser.setPassword("passHash");
        when(userManagementService.getUser(anyString())).thenReturn(existedUser);

        assertNull(validateAndGetResult(userProfileEditModel));

        userProfileEditModel.setChangePassword(new ChangePasswordModel());
        assertNull(validateAndGetResult(userProfileEditModel));
    }

    @Test
    void testEditUserProfile_password_notValid() {
        when(passwordEncoder.matches("pass", "passHash")).thenReturn(true);
        when(currentUserInfo.getUserName()).thenReturn("jsmith");
        var existedUser = new SimpleUser();
        existedUser.setPassword("passHash");
        var userProfileEditModel = getValidUserProfileEditModel();

        var changePasswordModel = new ChangePasswordModel().setNewPassword("pass2");
        userProfileEditModel.setChangePassword(changePasswordModel);
        when(userManagementService.getUser(anyString())).thenReturn(existedUser);
        var bindingResult = validateAndGetResult(userProfileEditModel);
        assertFieldError("changePassword",
                "Enter your password.",
                changePasswordModel,
                bindingResult.getFieldError("changePassword"));

        changePasswordModel = new ChangePasswordModel().setNewPassword("pass2")
                .setCurrentPassword("pass")
                .setConfirmPassword("pass1");
        userProfileEditModel.setChangePassword(changePasswordModel);
        bindingResult = validateAndGetResult(userProfileEditModel);
        assertFieldError("changePassword",
                "The new password and confirmed password do not match.",
                changePasswordModel,
                bindingResult.getFieldError("changePassword"));

        when(passwordEncoder.matches("pass", "passHash")).thenReturn(true);
        when(passwordEncoder.matches("pass1", "passHash1")).thenReturn(true);
        changePasswordModel = new ChangePasswordModel().setNewPassword("pass2")
                .setCurrentPassword("pass1")
                .setConfirmPassword("pass2");
        userProfileEditModel.setChangePassword(changePasswordModel);
        bindingResult = validateAndGetResult(userProfileEditModel);
        assertFieldError("changePassword",
                "Incorrect current password.",
                changePasswordModel,
                bindingResult.getFieldError("changePassword"));
    }

    @Test
    void testEditUserProfile_requiredFields() {
        var userProfileEditModel = getValidUserProfileEditModel()
                .setChangePassword(new ChangePasswordModel());

        userProfileEditModel.setEmail("");
        var bindingResult = validateAndGetResult(userProfileEditModel);
        assertFieldError("email", CANNOT_BE_EMPTY, "", bindingResult.getFieldError("email"));

        userProfileEditModel.setEmail("jsmith@email").setDisplayName(" ");
        bindingResult = validateAndGetResult(userProfileEditModel);
        assertFieldError("displayName", CANNOT_BE_EMPTY, " ", bindingResult.getFieldError("displayName"));
    }

    private UserCreateModel getValidUserCreateModel() {
        var groups = new HashSet<String>();
        groups.add("Administrators");
        return new UserCreateModel().setDisplayName("John Smith")
                .setFirstName("John")
                .setEmail("jsmith@email")
                .setLastName("Smith")
                .setInternalPassword(new InternalPasswordModel().setPassword("pass"))
                .setGroups(groups)
                .setUsername("jsmith");
    }

    private UserEditModel getValidUserEditModel() {
        var groups = new HashSet<String>();
        groups.add("Administrators");
        return new UserEditModel().setDisplayName("John Smith")
                .setFirstName("John")
                .setEmail("jsmith@email")
                .setLastName("Smith")
                .setPassword("pass")
                .setGroups(groups);
    }

    private UserInfoModel getValidUserInfoModel() {
        return new UserInfoModel().setDisplayName("John Smith")
                .setFirstName("John")
                .setEmail("jsmith@email")
                .setLastName("Smith");
    }

    private UserInfoEditModel getValidUserInfoEditModel() {
        return new UserInfoEditModel().setDisplayName("John Smith")
                .setFirstName("John")
                .setEmail("jsmith@email")
                .setLastName("Smith");
    }

    private UserProfileEditModel getValidUserProfileEditModel() {
        return new UserProfileEditModel().setChangePassword(
                        new ChangePasswordModel().setConfirmPassword("pass2").setNewPassword("pass2").setCurrentPassword("pass"))
                .setShowComplexResult(true)
                .setShowFormulas(true)
                .setShowRealNumbers(true)
                .setTestsFailuresOnly(true)
                .setTestsFailuresPerTest(20)
                .setTestsPerPage(20)
                .setShowHeader(true)
                .setDisplayName("John Smith")
                .setFirstName("John")
                .setEmail("jsmith@email")
                .setLastName("Smith");
    }

    private void assertOptionalNamesValid(UserInfoModel userInfoModel) {
        userInfoModel.setFirstName("").setLastName(" ");
        assertNull(validateAndGetResult(userInfoModel));
    }
}
