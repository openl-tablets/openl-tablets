package org.openl.rules.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import org.openl.rules.rest.exception.BadRequestException;
import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.rest.model.MailConfigModel;
import org.openl.rules.rest.model.NotificationModel;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.security.AdminPrivilege;
import org.openl.rules.security.OwnerOrAdminPrivilege;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.web.admin.MailVerificationServerSettings;
import org.openl.spring.env.DynamicPropertySource;

@RestController
@RequestMapping("/mail")
@Tag(name = "Mail")
public class MailController {

    public static final String MAIL_VERIFY_TOKEN = "mail.verify.token";

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

    @Operation(summary = "mail.verify.summary", description = "mail.verify.desc")
    @GetMapping("/verify/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Parameter(description = "mail.verify.param.token") @PathVariable("token") String token) {
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
        } else {
            throw new BadRequestException("mail.wrong.token");
        }
    }

    @Operation(summary = "mail.send-verification.summary", description = "mail.send-verification.desc")
    @PostMapping(value = "/send/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OwnerOrAdminPrivilege
    public NotificationModel sendVerification(HttpServletRequest request,
                                              @Parameter(description = "mail.send-verification.param.username") @PathVariable("username") String username) {
        User user = userManagementService.getUser(username);
        boolean emailWasSent = mailSender.sendVerificationMail(user, request);
        if (emailWasSent) {
            return new NotificationModel("Sent to " + user.getEmail());
        } else {
            throw new ForbiddenException("default.message");
        }
    }

    @Operation(summary = "mail.mail-config.summary", description = "mail.mail-config.desc")
    @GetMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated(forRemoval = true)
    public MailConfigModel getMailConfig() {
        return new MailConfigModel().setUrl(propertyResolver.getProperty(MailVerificationServerSettings.MAIL_URL))
                .setUsername(propertyResolver.getProperty(MailVerificationServerSettings.MAIL_USERNAME))
                .setPassword(propertyResolver.getProperty(MailVerificationServerSettings.MAIL_PASSWORD));
    }

    @Operation(summary = "mail.update-mail-config.summary", description = "mail.update-mail-config.desc")
    @PutMapping("/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AdminPrivilege
    @Deprecated(forRemoval = true)
    public void updateMailConfig(@RequestBody MailConfigModel mailConfig) throws IOException {
        validationProvider.validate(mailConfig);
        Map<String, String> mailConfigMap = new HashMap<>();
        mailConfigMap.put(MailVerificationServerSettings.MAIL_URL, mailConfig.getUrl());
        mailConfigMap.put(MailVerificationServerSettings.MAIL_USERNAME, mailConfig.getUsername());
        mailConfigMap.put(MailVerificationServerSettings.MAIL_PASSWORD, mailConfig.getPassword());
        DynamicPropertySource.get().save(mailConfigMap);
    }
}
