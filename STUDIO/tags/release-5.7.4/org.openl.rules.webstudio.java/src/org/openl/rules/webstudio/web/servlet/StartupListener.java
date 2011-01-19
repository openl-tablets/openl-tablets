package org.openl.rules.webstudio.web.servlet;

//import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

//import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class StartupListener implements HttpSessionListener {

    public void sessionCreated(HttpSessionEvent event) {
        //HttpSession session = event.getSession();

        //WebStudioUtils.getWebStudio(session, true);
    }

    public void sessionDestroyed(HttpSessionEvent event) {
    }

}
