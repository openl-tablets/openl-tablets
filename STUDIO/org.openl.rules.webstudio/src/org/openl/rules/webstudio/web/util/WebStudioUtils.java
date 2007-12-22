package org.openl.rules.webstudio.web.util;

import org.apache.commons.lang.StringUtils;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.JSFConst;
import org.openl.rules.webstudio.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;

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
    public static WebStudio getWebStudio() {
        return (WebStudio) (FacesUtils.getSessionMap().get("studio"));
    }

    public static RulesUserSession getRulesUserSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (RulesUserSession) session.getAttribute(JSFConst.RULES_USER_SESSION_ATTR);
    }

    /**
     * Checks if given ip address is loopback.
     *
     * @param ip ip address to check
     *
     * @return <code>true</code> if <code>ip</code> represents loopback address,
     *         <code>false</code> otherwise.
     */
    public static boolean isLoopbackAddress(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return (addr != null) && addr.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static boolean isLocalRequest(HttpServletRequest request) {
        String remote = request.getRemoteAddr();

        // TODO: think about proper implementation
        boolean b = isLoopbackAddress(remote);// || request.getLocalAddr().equals(remote);
        return b;
    }
}
