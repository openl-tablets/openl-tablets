package org.openl.rules.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.openl.rules.rest.exception.BadRequestException;
import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.rest.model.MailConfigModel;
import org.openl.rules.rest.model.NotificationModel;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.spring.env.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
public class MailController {

    public static final String MAIL_VERIFY_TOKEN = "mail.verify.token";
    public static final String MAIL_URL = "mail.url";
    public static final String MAIL_USERNAME = "mail.username";
    public static final String MAIL_PASSWORD = "mail.password";

    private final MailSender mailSender;
    private final UserSettingManagementService userSettingManagementService;
    private final UserManagementService userManagementService;
    private final CurrentUserInfo currentUserInfo;
    private final PropertyResolver propertyResolver;
    private final BeanValidationProvider validationProvider;

    @Autowired
    public MailController(MailSender mailSender,
            UserManagementService userManagementService,
            CurrentUserInfo currentUserInfo,
            UserSettingManagementService userSettingManagementService,
            PropertyResolver propertyResolver,
            BeanValidationProvider validationProvider) {
        this.mailSender = mailSender;
        this.userSettingManagementService = userSettingManagementService;
        this.userManagementService = userManagementService;
        this.currentUserInfo = currentUserInfo;
        this.propertyResolver = propertyResolver;
        this.validationProvider = validationProvider;
    }

    @GetMapping("/verify/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@PathVariable("token") String token) {
        String username = currentUserInfo.getUserName();
        User user = userManagementService.getUser(username);
        String dbToken = userSettingManagementService.getStringProperty(username, MAIL_VERIFY_TOKEN);
        if (Objects.equals(dbToken, token) && Objects.nonNull(user)) {
            userManagementService.updateUserData(user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                null,
                user.getEmail(),
                user.getDisplayName(),
                true);
            userSettingManagementService.setProperty(username, MAIL_VERIFY_TOKEN, "");
            updateCurrentUserEmailVerified();
        } else {
            throw new BadRequestException("mail.wrong.token");
        }
    }

    private void updateCurrentUserEmailVerified() {
        Optional.ofNullable(currentUserInfo.getAuthentication()).map(authentication -> {
            if (authentication.getPrincipal() instanceof SimpleUser) {
                return (SimpleUser) authentication.getPrincipal();
            } else if (authentication.getDetails() instanceof SimpleUser) {
                return (SimpleUser) authentication.getDetails();
            } else {
                return null;
            }
        })
            .ifPresent(
                simpleUser -> simpleUser.setExternalFlags(UserExternalFlags.builder(simpleUser.getExternalFlags())
                    .withFeature(UserExternalFlags.Feature.EMAIL_VERIFIED)
                    .build()));
    }

    @PostMapping(value = "/send/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public NotificationModel sendVerification(HttpServletRequest request, @PathVariable("username") String username) {
        if (!currentUserInfo.getUserName().equals(username)) {
            SecurityChecker.allow(Privileges.ADMIN);
        }
        User user = userManagementService.getUser(username);
        boolean emailWasSent = mailSender.sendVerificationMail(user, request);
        if (emailWasSent) {
            return new NotificationModel("Sent to " + user.getEmail());
        } else {
            throw new ForbiddenException("default.message");
        }
    }

    @GetMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    public MailConfigModel getMailConfig() {
        return new MailConfigModel().setUrl(propertyResolver.getProperty(MAIL_URL))
            .setUsername(propertyResolver.getProperty(MAIL_USERNAME))
            .setPassword(propertyResolver.getProperty(MAIL_PASSWORD));
    }

    @PutMapping("/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMailConfig(@RequestBody MailConfigModel mailConfig) throws IOException {
        SecurityChecker.allow(Privileges.ADMIN);
        validationProvider.validate(mailConfig);
        Map<String, String> mailConfigMap = new HashMap<>();
        mailConfigMap.put(MAIL_URL, mailConfig.getUrl());
        mailConfigMap.put(MAIL_USERNAME, mailConfig.getUsername());
        mailConfigMap.put(MAIL_PASSWORD, mailConfig.getPassword());
        DynamicPropertySource.get().save(mailConfigMap);
    }
}
