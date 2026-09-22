package org.openl.rules.webstudio.web.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.servlet.SpringInitializer;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * Contains utility methods, which can be used from any class.
 *
 * @author Aliaksandr Antonik
 */
@Slf4j
public final class WebStudioUtils {

    private WebStudioUtils() {
        // Utility class
    }

    public static RulesUserSession getRulesUserSession() {
        return getRulesUserSession(getSession());
    }

    public static RulesUserSession getRulesUserSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (RulesUserSession) session.getAttribute(Constants.RULES_USER_SESSION);
    }

    public static RulesUserSession getRulesUserSession(HttpSession session, boolean create) {
        if (session == null) {
            return null;
        }
        var rulesUserSession = getRulesUserSession(session);
        if (rulesUserSession == null && create) {
            ApplicationContext appContext = SpringInitializer.getApplicationContext(session.getServletContext());
            rulesUserSession = appContext.getBean(RulesUserSession.class);
            registerRulesUserSession(session, rulesUserSession);
        }
        return rulesUserSession;
    }

    public static void registerRulesUserSession(HttpSession session, RulesUserSession rulesUserSession) {
        session.setAttribute(Constants.RULES_USER_SESSION, rulesUserSession);
    }

    public static WebStudio getWebStudio() {
        var rulesUserSession = getRulesUserSession();
        return rulesUserSession == null ? null : rulesUserSession.getWebStudio();
    }

    public static WebStudio getWebStudio(HttpSession session) {
        var rulesUserSession = getRulesUserSession(session);
        return rulesUserSession == null ? null : rulesUserSession.getWebStudio();
    }


    public static UserWorkspace getUserWorkspace(HttpSession session) {
        UserWorkspace userWorkspace = null;
        try {
            var rulesUserSession = getRulesUserSession(session, true);
            if (rulesUserSession != null) {
                userWorkspace = rulesUserSession.getUserWorkspace();
            }
        } catch (Exception e) {
            log.error("Failed to get user workspace", e);
        }
        return userWorkspace;
    }

    public static HttpSession getSession() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getSession(false);
        }
        return null;
    }

}
