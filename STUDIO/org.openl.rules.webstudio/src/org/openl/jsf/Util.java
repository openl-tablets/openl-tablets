package org.openl.jsf;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.FacesUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Contains utility methods, which can be used from any class.
 *
 * @author Aliaksandr Antonik
 */
public abstract class Util {
    public static WebStudio getWebStudio() {
        return (WebStudio)(FacesUtils.getSessionMap().get("studio"));
    }

    /**
     * Checks if given ip address is loopback.
     *
     * @param ip ip address to check
     * @return <code>true</code> if <code>ip</code> represents loopback address, <code>false</code> otherwise. 
     */
    public static boolean isLoopbackAddress(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr != null && addr.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
