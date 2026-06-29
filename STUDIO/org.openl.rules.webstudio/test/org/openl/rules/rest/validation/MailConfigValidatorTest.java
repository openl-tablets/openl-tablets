package org.openl.rules.rest.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;

import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.BindingResult;

import org.openl.rules.rest.model.MailConfigModel;
import org.openl.rules.webstudio.mail.MailSender;
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
        SmtpServer smtp = smtpServer.getSmtp();
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
        assertNull(validateAndGetResult(getValidMailConfigModel()));
    }

    @Test
    void testMailConfig_emptyFields_notValid() {
        MailConfigModel mailConfigModel = getValidMailConfigModel();
        mailConfigModel.setUrl(null);
        BindingResult bindingResult = validateAndGetResult(mailConfigModel);
        assertEquals("Email server configuration fields cannot be empty.", bindingResult.getGlobalError().getDefaultMessage());
    }

    @Test
    void testMailConfig_wrongConfig_notValid() throws MessagingException {
        Transport transport = mock(Transport.class);
        when(transport.isConnected()).thenThrow(new IllegalArgumentException("Ho-ho-ho"));
        when(mailSender.getTransport(any(), any(), any())).thenReturn(transport);

        MailConfigModel mailConfigModel = getValidMailConfigModel();
        mailConfigModel.setUrl("127.0.0.2");
        BindingResult bindingResult = validateAndGetResult(mailConfigModel);
        assertEquals("Wrong email server configuration. Ho-ho-ho", bindingResult.getGlobalError().getDefaultMessage());
    }

    private MailConfigModel getValidMailConfigModel() {
        return new MailConfigModel()
                .setUrl(mailUrl)
                .setUsername("username@email")
                .setPassword("password");
    }
}
