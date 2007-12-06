package org.openl.rules.webstudio.web.servlet;

import org.openl.rules.webstudio.web.jsf.JSFConst;
import org.openl.util.Log;

import javax.servlet.http.*;

public class SessionListener implements HttpSessionActivationListener, HttpSessionBindingListener, HttpSessionListener {

    // Session Attribute

    public void sessionWillPassivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        System.out.println("sessionWillPassivate: " + session);
        printSession(session);

        RulesUserSession rulesUserSession = getUserRules(session);
        if (rulesUserSession != null) {
            rulesUserSession.sessionWillPassivate();
        }
    }

    public void sessionDidActivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        System.out.println("sessionDidActivate: " + session);
        printSession(session);

        RulesUserSession rulesUserSession = getUserRules(session);
        if (rulesUserSession != null) {
            rulesUserSession.sessionDidActivate();
        }
    }

    // Session Attribute

    public void valueBound(HttpSessionBindingEvent event) {
        HttpSession session = event.getSession();
        System.out.println("valueBound: " + session);
        printSession(session);
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
        HttpSession session = event.getSession();
        System.out.println("valueUnbound: " + session);
        printSession(session);
    }

// Global (one for all, in scope of web application)
//
//    place in web.xml
//
//    <listener>
//        <listener-class>org.openl.rules.webstudio.SessionListener</listener-class>
//    </listener>

    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        System.out.println("sessionCreated: " + session);
        printSession(session);

        Object obj = getUserRules(session);
        if (obj == null) {
            System.out.println("no rulesUserSession");
        } else {
            System.out.println("has rulesUserSession (why?)");
        }
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        System.out.println("sessionDestroyed: " + session);
        printSession(session);

        RulesUserSession obj = getUserRules(session);
        if (obj == null) {
            System.out.println("!!! no rulesUserSession");
        } else {
            Log.info("removing rulesUserSession");

            obj.sessionDestroyed();
            Log.info("session was destroyed");
        }
    }

    private RulesUserSession getUserRules(HttpSession session) {
        return (RulesUserSession) session.getAttribute(JSFConst.RULES_USER_SESSION_ATTR);
    }

    protected void printSession(HttpSession session) {
        System.out.println("  id           : " + session.getId());
        System.out.println("  creation time: " + session.getCreationTime());
        System.out.println("  accessed time: " + session.getLastAccessedTime());
        System.out.println("  max inactive : " + session.getMaxInactiveInterval());

        Object obj = getUserRules(session);
        System.out.println("  has rulesUserSession? " + (obj != null));
    }
}
