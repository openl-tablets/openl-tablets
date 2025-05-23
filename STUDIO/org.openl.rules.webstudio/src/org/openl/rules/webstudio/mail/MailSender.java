package org.openl.rules.webstudio.mail;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.servlet.http.HttpServletRequest;

import org.openl.rules.security.User;


/**
 * The implementation of SMTP client, that allows to send mails to SMTP Server.
 */
public interface MailSender {

    boolean sendVerificationMail(User user, HttpServletRequest httpServletRequest);

    Transport getTransport(String url, String user, String password) throws MessagingException;

}
