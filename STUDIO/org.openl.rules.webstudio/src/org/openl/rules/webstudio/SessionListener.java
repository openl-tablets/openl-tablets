package org.openl.rules.webstudio;

import org.openl.rules.webstudio.RulesUserSession;
import org.openl.rules.commons.logs.CLog;

import javax.servlet.http.*;

public class SessionListener implements HttpSessionActivationListener, HttpSessionBindingListener, HttpSessionListener {

    // Session Attribute

    public void sessionWillPassivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        System.out.println("sessionWillPassivate: " + session);
        printSession(session);
    }

    public void sessionDidActivate(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        System.out.println("sessionDidActivate: " + session);
        printSession(session);
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

        Object obj = session.getAttribute("rulesUserSession");
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

        Object obj = session.getAttribute("rulesUserSession");
        if (obj == null) {
            System.out.println("!!! no rulesUserSession");
        } else {
            CLog.log(CLog.INFO, "removing rulesUserSession");

            RulesUserSession rulesUserSession = (RulesUserSession) obj;
            rulesUserSession.sessionDestroyed();
            CLog.log(CLog.INFO, "session was destroyed");
        }
    }



    protected void printSession(HttpSession session) {
        System.out.println("  id           : " + session.getId());
        System.out.println("  creation time: " + session.getCreationTime());
        System.out.println("  accessed time: " + session.getLastAccessedTime());
        System.out.println("  max inactive : " + session.getMaxInactiveInterval());

        Object obj = session.getAttribute("rulesUserSession");
        System.out.println("  has rulesUserSession? " + (obj != null));
    }
}
