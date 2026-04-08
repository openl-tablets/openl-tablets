package org.openl.rules.webstudio.web.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.web.context.support.SecurityWebApplicationContextUtils;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.security.web.session.HttpSessionIdChangedEvent;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

@Slf4j
public class SessionListener implements HttpSessionActivationListener, HttpSessionListener, HttpSessionIdListener {


    private static ApplicationContext getContext(ServletContext servletContext) {
        return SecurityWebApplicationContextUtils.findRequiredWebApplicationContext(servletContext);
    }

    private RulesUserSession getUserRules(HttpSession session) {
        return (RulesUserSession) session.getAttribute(Constants.RULES_USER_SESSION);
    }

    private void printSession(HttpSession session) {
        if (log.isDebugEnabled()) {
            long creationTime = 0;
            try {
                creationTime = session.getCreationTime();
            } catch (IllegalStateException e) {
                log.debug("Session is invalidated, cannot get Creation Time.");
            }

            long lastAccessedTime = 0;
            try {
                lastAccessedTime = session.getLastAccessedTime();
            } catch (IllegalStateException e) {
                log.debug("Session is invalidated, cannot get Last Accessed Time.");
            }

            log.debug(
                    """

                      id           : {}
                      creation time: {}
                      accessed time: {}
                      max inactive : {}
                      has rulesUserSession? {}\
                    """,
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
        publishSessionEvent(session, new HttpSessionCreatedEvent(session));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionDestroyed: {}", session);
        printSession(session);
        SpringInitializer.removeSessionCache(session, session.getId());
        publishSessionEvent(session, new HttpSessionDestroyedEvent(session));

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

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        HttpSession session = event.getSession();
        log.debug("sessionIdChanged: {}", session);
        printSession(session);

        SpringInitializer.removeSessionCache(session, oldSessionId);
        SpringInitializer.addSessionCache(session);
        publishSessionEvent(session, new HttpSessionIdChangedEvent(session, oldSessionId));
    }

    private void publishSessionEvent(HttpSession session, ApplicationEvent e) {
        log.debug("Publishing event: {}", e);
        getContext(session.getServletContext()).publishEvent(e);
    }
}
