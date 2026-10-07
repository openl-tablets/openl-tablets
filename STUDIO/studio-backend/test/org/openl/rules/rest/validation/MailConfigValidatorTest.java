package org.openl.rules.rest.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.web.admin.MailVerificationServerSettings;
import org.openl.studio.common.validation.AbstractConstraintValidatorTest;


@SpringJUnitConfig(classes = MockConfiguration.class)
class MailConfigValidatorTest extends AbstractConstraintValidatorTest {

    private static GreenMail smtpServer;
    private static String mailUrl;

    @Autowired
    private MailSender mailSender;

    @AfterEach
    void reset_mocks() {
        reset(mailSender);
    }

    @BeforeAll
    static void setUp() {
        smtpServer = new GreenMail(new ServerSetup(0, null, ServerSetup.PROTOCOL_SMTP));
        smtpServer.setUser("username@email", "password");
        smtpServer.start();
        var smtp = smtpServer.getSmtp();
        mailUrl = smtp.getProtocol() + "://" + smtp.getBindTo() + ":" + smtp.getPort();

    }

    @AfterAll
    static void tearDown() {
        smtpServer.stop();
    }

    @Test
    void testMailConfig_valid() throws MessagingException {
        Transport transport = mock(Transport.class);
        when(transport.isConnected()).thenReturn(true);
        when(mailSender.getTransport(any(), any(), any())).thenReturn(transport);
        assertNull(validateAndGetResult(getValidMailSettings()));
    }

    @Test
    void testMailConfig_emptyFields_notValid() {
        var mailSettings = getValidMailSettings();
        mailSettings.setUrl(null);
        var bindingResult = validateAndGetResult(mailSettings);
        assertEquals("Email server configuration fields cannot be empty.", bindingResult.getGlobalError().getDefaultMessage());
    }

    @Test
    void testMailConfig_wrongConfig_notValid() throws MessagingException {
        Transport transport = mock(Transport.class);
        when(transport.isConnected()).thenThrow(new IllegalArgumentException("Ho-ho-ho"));
        when(mailSender.getTransport(any(), any(), any())).thenReturn(transport);

        var mailSettings = getValidMailSettings();
        mailSettings.setUrl("127.0.0.2");
        var bindingResult = validateAndGetResult(mailSettings);
        assertEquals("Wrong email server configuration. Ho-ho-ho", bindingResult.getGlobalError().getDefaultMessage());
    }

    private static MailVerificationServerSettings getValidMailSettings() {
        var mailSettings = new MailVerificationServerSettings();
        mailSettings.setUrl(mailUrl);
        mailSettings.setUsername("username@email");
        mailSettings.setPassword("password");
        return mailSettings;
    }
}
