package org.openl.rules.webstudio.web.util;

import javax.servlet.http.HttpSession;

import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.JSFConst;
import org.openl.rules.webstudio.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;

/**
 * Contains utility methods, which can be used from any class.
 *
 * @author Aliaksandr Antonik
 */
public abstract class WebStudioUtils {
    public static WebStudio getWebStudio() {
        return (WebStudio) (FacesUtils.getSessionMap().get("studio"));
    }

    public static WebStudio getWebStudio(HttpSession session) {
        return session == null ? null : (WebStudio) session.getAttribute("studio");
    }

    public static RulesUserSession getRulesUserSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (RulesUserSession) session.getAttribute(JSFConst.RULES_USER_SESSION_ATTR);
    }

    public static boolean isRepositoryFailed() {
        return RulesRepositoryFactory.isFailed();
    }
}
