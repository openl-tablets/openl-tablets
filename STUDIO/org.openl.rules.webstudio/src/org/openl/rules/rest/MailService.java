package org.openl.rules.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

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
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@Path("/mail")
public class MailService {

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

    @Inject
    public MailService(MailSender mailSender,
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

    @GET
    @Path("/verify/{token}")
    public void verify(@PathParam("token") String token) {
        String username = currentUserInfo.getUserName();
        User user = userManagementService.getUser(username);
        String dbToken = userSettingManagementService.getStringProperty(username, MAIL_VERIFY_TOKEN);
        if (Objects.equals(dbToken, token) && Objects.nonNull(user)) {
            userManagementService.updateUserData(user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                null,
                false,
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

    @POST
    @Path("/send/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public NotificationModel sendVerification(@Context UriInfo uriInfo, @PathParam("username") String username) {
        if (!currentUserInfo.getUserName().equals(username)) {
            SecurityChecker.allow(Privileges.ADMIN);
        }
        User user = userManagementService.getUser(username);
        boolean emailWasSent = mailSender.sendVerificationMail(user, uriInfo);
        if (emailWasSent) {
            return new NotificationModel("Sent to " + user.getEmail());
        } else {
            throw new ForbiddenException("default.message");
        }
    }

    @GET
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON)
    public MailConfigModel getMailConfig() {
        return new MailConfigModel()
            .setUrl(propertyResolver.getProperty(MAIL_URL))
            .setUsername(propertyResolver.getProperty(MAIL_USERNAME))
            .setPassword(propertyResolver.getProperty(MAIL_PASSWORD));
    }

    @PUT
    @Path("/settings")
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
