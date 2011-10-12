package org.openl.rules.webstudio.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import javax.servlet.http.*;

public class SessionListener implements HttpSessionActivationListener, HttpSessionListener {
    private static final Log LOG = LogFactory.getLog(SessionListener.class);

    // Session Attribute

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

        LOG.debug(sb.toString());
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
        LOG.debug("sessionCreated: " + session);
        printSession(session);

        Object obj = getUserRules(session);
        if (obj == null) {
            LOG.debug("no rulesUserSession");
        } else {
            LOG.debug("has rulesUserSession (why?)");
        }

        WebStudioUtils.getWebStudio(event.getSession(), true);
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        LOG.debug("sessionDestroyed: " + session);
        printSession(session);

        RulesUserSession obj = getUserRules(session);
        if (obj == null) {
            LOG.debug("!!! no rulesUserSession");
        } else {
            LOG.debug("removing rulesUserSession");

            obj.sessionDestroyed();
            LOG.debug("session was destroyed");
        }
    }

    public void sessionDidActivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        LOG.debug("sessionDidActivate: " + session);
        printSession(session);

        RulesUserSession rulesUserSession = getUserRules(session);
        if (rulesUserSession != null) {
            rulesUserSession.sessionDidActivate();
        }
    }

    public void sessionWillPassivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        LOG.debug("sessionWillPassivate: " + session);
        printSession(session);

        RulesUserSession rulesUserSession = getUserRules(session);
        if (rulesUserSession != null) {
            rulesUserSession.sessionWillPassivate();
        }
    }
}
