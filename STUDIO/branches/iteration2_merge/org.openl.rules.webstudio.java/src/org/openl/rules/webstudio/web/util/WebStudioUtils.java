package org.openl.rules.webstudio.web.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.JSFConst;
import org.openl.rules.webstudio.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.repository.RulesRepositoryFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


/**
 * Contains utility methods, which can be used from any class.
 *
 * @author Aliaksandr Antonik
 */
public abstract class WebStudioUtils {
    private static final Log log = LogFactory.getLog(WebStudioUtils.class);

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
