package org.openl.rules.webstudio.web.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class SessionListener implements HttpSessionActivationListener, HttpSessionListener {
    private static final String SERVLET_CONTEXT_KEY = "SessionCache";

    private final Log log = LogFactory.getLog(SessionListener.class);

    private RulesUserSession getUserRules(HttpSession session) {
        return (RulesUserSession) session.getAttribute(Constants.RULES_USER_SESSION);
    }

    protected void printSession(HttpSession session) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("\n  id           : " + session.getId());
        sb.append("\n  creation time: " + session.getCreationTime());
        sb.append("\n  accessed time: " + session.getLastAccessedTime());
        sb.append("\n  max inactive : " + session.getMaxInactiveInterval());

        Object obj = getUserRules(session);
        sb.append("\n  has rulesUserSession? " + (obj != null));

        log.debug(sb.toString());
    }

    // Global (one for all, in scope of web application)
    //
    // place in web.xml
    //
    // <listener>
    // <listener-class>org.openl.rules.webstudio.SessionListener</listener-class>
    // </listener>

    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionCreated: " + session);
        printSession(session);

        getSessionCache(event).add(session);

        Object obj = getUserRules(session);
        if (obj == null) {
            log.debug("no rulesUserSession");
        } else {
            log.debug("has rulesUserSession (why?)");
        }
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionDestroyed: " + session);
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

    public void sessionDidActivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionDidActivate: " + session);
        printSession(session);

        RulesUserSession rulesUserSession = getUserRules(session);
        if (rulesUserSession != null) {
            rulesUserSession.sessionDidActivate();
        }
    }

    public void sessionWillPassivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("sessionWillPassivate: " + session);
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
