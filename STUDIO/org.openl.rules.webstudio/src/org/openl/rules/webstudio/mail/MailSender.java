package org.openl.rules.webstudio.mail;

import static org.openl.rules.rest.MailController.MAIL_VERIFY_TOKEN;

import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


/**
 * The implementation of SMTP client, that allows to send mails to SMTP Server.
 */
@Service
public class MailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailSender.class);

    private static final String MAIL_VERIFICATION_TEMPLATE = "/templates/email-verification.eml";
    private static final String MAIL_VERIFICATION_PAGE = "/faces/pages/modules/emailVerification.xhtml";

    @Value("${mail.url}")
    private String url;

    @Value("${mail.username}")
    private String user;

    @Value("${mail.password}")
    private String password;

    private final UserSettingManagementService userSettingManagementService;

    @Autowired
    public MailSender(UserSettingManagementService userSettingManagementService) {
        this.userSettingManagementService = userSettingManagementService;
    }

    /**
     * Send e-mail for verification for the given user
     *
     * @return true - if e-mail has been sent successful, false - if the service is off.
     */
    public boolean sendVerificationMail(User user, HttpServletRequest httpServletRequest) {
        String token = RandomStringUtils.random(8, false, true);
        boolean emailWasSent = false;

        String verificationLink = createVerificationLink(httpServletRequest, token);

        try {
            if (isValidEmailSettings()) {

                // Read template
                String template;
                try (var templateStream = getClass().getResourceAsStream(MAIL_VERIFICATION_TEMPLATE)) {
                    template = IOUtils.toStringAndClose(templateStream);
                }

                // substitute values
                var emailContent = template
                        .replace("${link}", verificationLink)
                        .replace("${from}", this.user)
                        .replace("${user}", user.getFirstName())
                        .replace("${email}", user.getEmail());

                // Parse eml template
                var msg = new MimeMessage(null, IOUtils.toInputStream(emailContent));
                msg.saveChanges();

                // Send email
                try (Transport transport = getTransport(url, this.user, password)) {
                    transport.sendMessage(msg, msg.getAllRecipients());
                    userSettingManagementService.setProperty(user.getUsername(), MAIL_VERIFY_TOKEN, token);
                    emailWasSent = true;
                }
            }

        } catch (MessagingException | IOException e) {
            LOGGER.error("Mail message preparation failed: ", e);
        }
        return emailWasSent;
    }

    public Transport getTransport(String url, String user, String password) throws MessagingException {
        Properties props = new Properties();
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.smtps.starttls.enable", "true");
        Session session = Session.getInstance(props);
        Transport transport = session.getTransport(new URLName(url));
        transport.connect(user, password);
        return transport;
    }

    private String createVerificationLink(HttpServletRequest httpServletRequest, String token) {
        String root = httpServletRequest.getRequestURL().toString();
        if (root.contains("/web")) {
            root = root.substring(0, root.lastIndexOf("/web"));
        } else if (root.contains("/rest")) {
            root = root.substring(0, root.lastIndexOf("/rest"));
        }
        return root + MAIL_VERIFICATION_PAGE + "?token=" + token;
    }

    public boolean isValidEmailSettings() {
        return StringUtils.isNotBlank(url) && StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password);
    }
}
