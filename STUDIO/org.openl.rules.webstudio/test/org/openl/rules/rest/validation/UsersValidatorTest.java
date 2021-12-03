package org.openl.rules.rest.validation;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openl.rules.rest.model.ChangePasswordModel;
import org.openl.rules.rest.model.InternalPasswordModel;
import org.openl.rules.rest.model.UserCreateModel;
import org.openl.rules.rest.model.UserEditModel;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.rest.model.UserProfileEditModel;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BindingResult;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MockConfiguration.class)
public class UsersValidatorTest extends AbstractConstraintValidatorTest {

    private static final String MUST_BE_LESS_THAN_25 = "Must be less than 25.";
    private static final String CANNOT_BE_EMPTY = "Cannot be empty.";
    private static final String SHOULD_NOT_END_WITH_DOT_AND_WHITESPACE = "The name should not end or begin with '.' or a whitespace.";
    private static final String MUST_NOT_CONTAIN_FOLLOWING_CHARS = "The name must not contain the following characters: /  : * ? \" < > | { } ~ ^";

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private CurrentUserInfo currentUserInfo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @After
    public void reset_mocks() {
        Mockito.reset(userManagementService);
        Mockito.reset(passwordEncoder);
        Mockito.reset(currentUserInfo);
    }

    @Test
    public void testUserInfo_valid() {
        assertNull(validateAndGetResult(getValidUserInfoModel()));
    }

    @Test
    public void testUserInfo_firstName_notValid() {
        UserInfoModel userInfoModel = getValidUserInfoModel();
        String wrongFirstName = RandomStringUtils.random(26, "John");
        userInfoModel.setFirstName(wrongFirstName);
        BindingResult bindingResult = validateAndGetResult(userInfoModel);
        assertFieldError("firstName", MUST_BE_LESS_THAN_25, wrongFirstName, bindingResult.getFieldError("firstName"));
    }

    @Test
    public void testUserInfo_lastName_notValid() {
        UserInfoModel userInfoModel = getValidUserInfoModel();
        String wrongLastName = RandomStringUtils.random(26, "Smith");
        userInfoModel.setLastName(wrongLastName);
        BindingResult bindingResult = validateAndGetResult(userInfoModel);
        assertFieldError("lastName", MUST_BE_LESS_THAN_25, wrongLastName, bindingResult.getFieldError("lastName"));
    }

    @Test
    public void testUserInfo_displayName_notValid() {
        UserInfoModel userInfoModel = getValidUserInfoModel();
        String wrongDisplayName = RandomStringUtils.random(65, "John Smith");
        userInfoModel.setDisplayName(wrongDisplayName);
        BindingResult bindingResult = validateAndGetResult(userInfoModel);
        assertFieldError("displayName",
            "Must be less than 64.",
            wrongDisplayName,
            bindingResult.getFieldError("displayName"));
    }

    @Test
    public void testUserInfo_email_notValid() {
        UserInfoModel userInfoModel = getValidUserInfoModel();
        userInfoModel.setEmail("wrongEmail");
        BindingResult bindingResult = validateAndGetResult(userInfoModel);
        assertFieldError("email", "Email address is invalid.", "wrongEmail", bindingResult.getFieldError("email"));
    }

    @Test
    public void testEditUser_valid() {
        UserEditModel userEditModel = getValidUserEditModel();

        assertNull(validateAndGetResult(userEditModel));

        userEditModel.setPassword(null);
        assertNull(validateAndGetResult(userEditModel));
    }

    @Test
    public void testEditUser_password_notValid() {
        UserEditModel userEditModel = getValidUserEditModel();
        String wrongPassword = RandomStringUtils.random(26, "pass");
        userEditModel.setPassword(wrongPassword);
        BindingResult bindingResult = validateAndGetResult(userEditModel);
        assertFieldError("password", MUST_BE_LESS_THAN_25, wrongPassword, bindingResult.getFieldError("password"));
    }

    @Test
    public void testCreateUser_valid() {
        when(userManagementService.getApplicationUser(anyString())).thenReturn(null);
        UserCreateModel userCreateModel = getValidUserCreateModel();

        assertNull(validateAndGetResult(getValidUserCreateModel()));

        userCreateModel.setInternalPassword(new InternalPasswordModel().setInternalUser(false).setPassword(null));
        assertNull(validateAndGetResult(userCreateModel));

        userCreateModel.setUsername("a 1!@#$%&()_-+=;'.,");
        assertNull(validateAndGetResult(userCreateModel));

        userCreateModel.setUsername("фы漢語, 汉语");
        assertNull(validateAndGetResult(userCreateModel));

        userCreateModel.setUsername("a");
        assertNull(validateAndGetResult(userCreateModel));
    }

    @Test
    public void testCreateUser_password_notValid() {
        when(userManagementService.getApplicationUser(anyString())).thenReturn(null);
        UserCreateModel userCreateModel = getValidUserCreateModel();

        InternalPasswordModel wrongInternalPassword = new InternalPasswordModel().setInternalUser(true)
            .setPassword(null);
        userCreateModel.setInternalPassword(wrongInternalPassword);
        BindingResult bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("internalPassword",
            CANNOT_BE_EMPTY,
            wrongInternalPassword,
            bindingResult.getFieldError("internalPassword"));

        wrongInternalPassword = new InternalPasswordModel().setInternalUser(true)
            .setPassword(RandomStringUtils.random(26, "pass"));
        userCreateModel.setInternalPassword(wrongInternalPassword);
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("internalPassword",
            MUST_BE_LESS_THAN_25,
            wrongInternalPassword,
            bindingResult.getFieldError("internalPassword"));
    }

    @Test
    public void testCreateUser_username_notValid() {
        when(userManagementService.getApplicationUser(anyString())).thenReturn(null);
        UserCreateModel userCreateModel = getValidUserCreateModel();

        String wrongUsername = RandomStringUtils.random(26, "jsmith");
        userCreateModel.setUsername(wrongUsername);
        BindingResult bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", MUST_BE_LESS_THAN_25, wrongUsername, bindingResult.getFieldError("username"));

        userCreateModel.setUsername(null);
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username", CANNOT_BE_EMPTY, null, bindingResult.getFieldError("username"));

        userCreateModel.setUsername("a..aa");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
            "The name should not contain consecutive '.'.",
            "a..aa",
            bindingResult.getFieldError("username"));

        userCreateModel.setUsername(".aa");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
            SHOULD_NOT_END_WITH_DOT_AND_WHITESPACE,
            ".aa",
            bindingResult.getFieldError("username"));

        userCreateModel.setUsername("aa.");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
            SHOULD_NOT_END_WITH_DOT_AND_WHITESPACE,
            "aa.",
            bindingResult.getFieldError("username"));

        userCreateModel.setUsername(" aa");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
            SHOULD_NOT_END_WITH_DOT_AND_WHITESPACE,
            " aa",
            bindingResult.getFieldError("username"));

        userCreateModel.setUsername("aa ");
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
            SHOULD_NOT_END_WITH_DOT_AND_WHITESPACE,
            "aa ",
            bindingResult.getFieldError("username"));

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

        userCreateModel.setUsername("jsmith");
        when(userManagementService.getApplicationUser(anyString())).thenReturn(new SimpleUser());
        bindingResult = validateAndGetResult(userCreateModel);
        assertFieldError("username",
            "User with such username already exists.",
            "jsmith",
            bindingResult.getFieldError("username"));

    }

    @Test
    public void testEditUserProfile_valid() {
        UserProfileEditModel userProfileEditModel = getValidUserProfileEditModel();
        when(currentUserInfo.getUserName()).thenReturn("jsmith");
        when(passwordEncoder.matches("pass", "passHash")).thenReturn(true);
        SimpleUser existedUser = new SimpleUser();
        existedUser.setPassword("passHash");
        when(userManagementService.getApplicationUser(anyString())).thenReturn(existedUser);

        assertNull(validateAndGetResult(userProfileEditModel));

        userProfileEditModel.setChangePassword(new ChangePasswordModel());
        assertNull(validateAndGetResult(userProfileEditModel));
    }

    @Test
    public void testEditUserProfile_password_notValid() {
        when(passwordEncoder.matches("pass", "passHash")).thenReturn(true);
        when(currentUserInfo.getUserName()).thenReturn("jsmith");
        SimpleUser existedUser = new SimpleUser();
        existedUser.setPassword("passHash");
        UserProfileEditModel userProfileEditModel = getValidUserProfileEditModel();

        ChangePasswordModel changePasswordModel = new ChangePasswordModel().setNewPassword("pass2");
        userProfileEditModel.setChangePassword(changePasswordModel);
        when(userManagementService.getApplicationUser(anyString())).thenReturn(existedUser);
        BindingResult bindingResult = validateAndGetResult(userProfileEditModel);
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
            "New password and confirm password do not match.",
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

    private UserCreateModel getValidUserCreateModel() {
        Set<String> groups = new HashSet<>();
        groups.add("Administrators");
        return new UserCreateModel().setDisplayName("John Smith")
            .setFirstName("John")
            .setEmail("jsmith@email")
            .setLastName("Smith")
            .setInternalPassword(new InternalPasswordModel().setPassword("pass").setInternalUser(true))
            .setGroups(groups)
            .setUsername("jsmith");
    }

    private UserEditModel getValidUserEditModel() {
        Set<String> groups = new HashSet<>();
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

}
