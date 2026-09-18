package org.openl.rules.webstudio.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins the address a verification e-mail points the recipient at.
 *
 * @author Yury Molchan
 */
class MailSenderTest {

    private static final String TOKEN = "12345678";

    @Test
    void linkDropsTheApiPrefixOfARootDeployment() {
        assertEquals("http://openl.example.com/email?token=" + TOKEN,
                MailSender.createVerificationLink("http://openl.example.com/rest/mail/send/admin", TOKEN));
    }

    @Test
    void linkKeepsAContextPathThatContainsTheWordWeb() {
        // "/webstudio" used to be cut here, because it contains the former "/web" prefix (EPBDS-16690).
        assertEquals("http://openl.example.com:8080/webstudio/email?token=" + TOKEN,
                MailSender.createVerificationLink("http://openl.example.com:8080/webstudio/rest/mail/send/admin",
                        TOKEN));
    }

    @Test
    void linkKeepsAContextPathThatEndsWithTheApiPrefix() {
        assertEquals("http://openl.example.com/rest/email?token=" + TOKEN,
                MailSender.createVerificationLink("http://openl.example.com/rest/rest/mail/send/admin", TOKEN));
    }

    @Test
    void addressWithoutTheApiPrefixIsTakenAsTheRoot() {
        assertEquals("http://openl.example.com/email?token=" + TOKEN,
                MailSender.createVerificationLink("http://openl.example.com", TOKEN));
    }
}
