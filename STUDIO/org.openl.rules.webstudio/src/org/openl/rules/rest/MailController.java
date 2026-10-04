package org.openl.rules.rest;

import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.security.CurrentUserInfo;
import org.openl.studio.security.OwnerOrAdminPrivilege;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mail")
@Tag(name = "Mail")
public class MailController {

    public static final String MAIL_VERIFY_TOKEN = "mail.verify.token";

    private final MailSender mailSender;
    private final UserSettingManagementService userSettingManagementService;
    private final UserManagementService userManagementService;
    private final CurrentUserInfo currentUserInfo;

    @Operation(summary = "mail.verify.summary", description = "mail.verify.desc")
    @GetMapping("/verify/{token}")
    public void verify(@Parameter(description = "mail.verify.param.token") @PathVariable("token") String token) {
        var username = currentUserInfo.getUserName();
        var user = userManagementService.getUser(username);
        var dbToken = userSettingManagementService.getStringProperty(username, MAIL_VERIFY_TOKEN);
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
    @PostMapping("/send/{username}")
    @OwnerOrAdminPrivilege
    public void sendVerification(HttpServletRequest request,
                                 @Parameter(description = "mail.send-verification.param.username") @PathVariable("username") String username) {
        var user = userManagementService.getUser(username);
        var emailWasSent = mailSender.sendVerificationMail(user, request);
        if (!emailWasSent) {
            throw new ForbiddenException("default.message");
        }
    }
}
