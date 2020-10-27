package org.openl.rules.webstudio.web.servlet;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionListener implements HttpSessionActivationListener, HttpSessionListener {
    private static final String SERVLET_CONTEXT_KEY = "SessionCache";

    private final Logger log = LoggerFactory.getLogger(SessionListener.class);

    private RulesUserSession getUserRules(HttpSession session) {
        return (RulesUserSession) session.getAttribute(Constants.RULES_USER_SESSION);
    }

    private void printSession(HttpSession session) {
        if (log.isDebugEnabled()) {
            long creationTime = 0;
            try {
                creationTime = session.getCreationTime();
            } catch (IllegalStateException e) {
                log.debug("Session is invalidated, can't get Creation Time.");
            }

            long lastAccessedTime = 0;
            try {
                lastAccessedTime = session.getLastAccessedTime();
            } catch (IllegalStateException e) {
                log.debug("Session is invalidated, can't get Last Accessed Time.");
            }

            log.debug(
                    "\n" + "  id           : {}\n" + "  creation time: {}\n" + "  accessed time: {}\n" + "  max inactive : {}\n" + "  has rulesUserSession? {}",
                    session.getId(),
                    creationTime,
                    lastAccessedTime,
                    session.getMaxInactiveInterval(),
                    getUserRules(session) != null);
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        printSession(session);
        log.debug("sessionCreated: {}", session);
        SpringInitializer.addSessionCache(session);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionDestroyed: {}", session);
        printSession(session);
        SpringInitializer.removeSessionCache(session);

        RulesUserSession obj = getUserRules(session);
        if (obj == null) {
            log.debug("!!! no rulesUserSession");
        } else {
            log.debug("removing rulesUserSession");

            obj.sessionDestroyed();
            log.debug("session was destroyed");
        }

        WebStudio webStudio = WebStudioUtils.getWebStudio(session);
        if (webStudio != null) {
            webStudio.destroy();
        }
    }

    @Override
    public void sessionDidActivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionDidActivate: {}", session);
        printSession(session);

        RulesUserSession rulesUserSession = getUserRules(session);
        if (rulesUserSession != null) {
            rulesUserSession.sessionDidActivate();
        }
    }

    @Override
    public void sessionWillPassivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionWillPassivate: {}", session);
        printSession(session);

        RulesUserSession rulesUserSession = getUserRules(session);
        if (rulesUserSession != null) {
            rulesUserSession.sessionWillPassivate();
        }
    }
}
