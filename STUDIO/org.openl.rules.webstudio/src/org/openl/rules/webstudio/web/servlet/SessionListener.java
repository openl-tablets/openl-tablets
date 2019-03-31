package org.openl.rules.webstudio.web.servlet;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
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

    protected void printSession(HttpSession session) {
        log.debug(
            "\n" + "  id           : {}\n" + "  creation time: {}\n" + "  accessed time: {}\n" + "  max inactive : {}\n" + "  has rulesUserSession? {}",
            session.getId(),
            session.getCreationTime(),
            session.getLastAccessedTime(),
            session.getMaxInactiveInterval(),
            getUserRules(session) != null);
    }

    // Global (one for all, in scope of web application)
    //
    // place in web.xml
    //
    // <listener>
    // <listener-class>org.openl.rules.webstudio.SessionListener</listener-class>
    // </listener>

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionCreated: {}", session);
        printSession(session);

        getSessionCache(event).add(session);

        Object obj = getUserRules(session);
        if (obj == null) {
            log.debug("no rulesUserSession");
        } else {
            log.debug("has rulesUserSession (why?)");
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionDestroyed: {}", session);
        printSession(session);

        getSessionCache(event).remove(session);

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

    public static SessionCache getSessionCache(ServletContext context) {
        return (SessionCache) context.getAttribute(SERVLET_CONTEXT_KEY);
    }

    private SessionCache getSessionCache(HttpSessionEvent event) {
        ServletContext servletContext = event.getSession().getServletContext();
        SessionCache cache = getSessionCache(servletContext);

        if (cache == null) {
            cache = new SessionCache();
            servletContext.setAttribute(SERVLET_CONTEXT_KEY, cache);
        }

        return cache;
    }
}
