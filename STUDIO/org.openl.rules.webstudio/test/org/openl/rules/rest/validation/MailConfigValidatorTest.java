package org.openl.rules.rest.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.mail.MessagingException;
import javax.mail.Transport;

import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BindingResult;

import org.openl.rules.rest.model.MailConfigModel;
import org.openl.rules.webstudio.mail.MailSender;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MockConfiguration.class)
public class MailConfigValidatorTest extends AbstractConstraintValidatorTest {

    private static GreenMail smtpServer;
    private static String mailUrl;

    @Autowired
    private MailSender mailSender;

    @After
    public void reset_mocks() {
        Mockito.reset(mailSender);
    }

    @BeforeClass
    public static void setUp() {
        smtpServer = new GreenMail(new ServerSetup(0, null, ServerSetup.PROTOCOL_SMTP));
        smtpServer.setUser("username@email", "password");
        smtpServer.start();
        SmtpServer smtp = smtpServer.getSmtp();
        mailUrl = smtp.getProtocol() + "://" + smtp.getBindTo() + ":" + smtp.getPort();

    }

    @AfterClass
    public static void tearDown() {
        smtpServer.stop();
    }

    @Test
    public void testMailConfig_valid() throws MessagingException {
        Transport transport = mock(Transport.class);
        when(transport.isConnected()).thenReturn(true);
        when(mailSender.getTransport(any(), any(), any())).thenReturn(transport);
        assertNull(validateAndGetResult(getValidMailConfigModel()));
    }

    @Test
    public void testMailConfig_emptyFields_notValid() {
        MailConfigModel mailConfigModel = getValidMailConfigModel();
        mailConfigModel.setUrl(null);
        BindingResult bindingResult = validateAndGetResult(mailConfigModel);
        assertEquals("Email server configuration fields cannot be empty.", bindingResult.getGlobalError().getDefaultMessage());
    }

    @Test
    public void testMailConfig_wrongConfig_notValid() throws MessagingException {
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
