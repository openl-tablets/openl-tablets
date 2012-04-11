package org.openl.rules.webstudio.web.util;

import java.util.Map;
import javax.servlet.http.HttpSession;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;

/**
 * Contains utility methods, which can be used from any class.
 *
 * @author Aliaksandr Antonik
 */
public abstract class WebStudioUtils {
    private static final String STUDIO_ATTR = "studio";

    public static RulesUserSession getRulesUserSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (RulesUserSession) session.getAttribute(Constants.RULES_USER_SESSION);
    }

    public static WebStudio getWebStudio() {
        return (WebStudio) (FacesUtils.getSessionParam(STUDIO_ATTR));
    }

    public static WebStudio getWebStudio(boolean create) {
        if (create) {
            WebStudio studio = getWebStudio();
            if (studio != null) {
                return studio;
            }

            Map<String, Object> sessionMap = FacesUtils.getSessionMap();
            synchronized (sessionMap) {
                WebStudio webStudio = getWebStudio();
                if (webStudio == null) {
                    sessionMap.put(STUDIO_ATTR, webStudio = new WebStudio());
                }
                return webStudio;
            }
        } else {
            return getWebStudio();
        }
    }

    public static WebStudio getWebStudio(HttpSession session) {
        return session == null ? null : (WebStudio) session.getAttribute(STUDIO_ATTR);
    }

    public static boolean isRepositoryFailed() {
        return RulesRepositoryFactory.isFailed();
    }

    public static boolean isStudioReady() {
        WebStudio webStudio = getWebStudio();
        return webStudio != null && webStudio.getModel().isReady();
    }

    public static ProjectModel getProjectModel() {
        WebStudio webStudio = getWebStudio();
        if (webStudio != null) {
            return webStudio.getModel();
        }
        return null;
    }

}
