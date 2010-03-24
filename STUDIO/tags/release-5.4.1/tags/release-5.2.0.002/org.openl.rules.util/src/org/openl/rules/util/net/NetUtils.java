package org.openl.rules.util.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Aliaksandr Antonik.
 */
public final class NetUtils {
    private static final Log log = LogFactory.getLog(NetUtils.class);

    /**
     * Checks if given IP address is a loopback.
     *
     * @param ip address to check
     *
     * @return <code>true</code> if <code>ip</code> represents loopback
     *         address, or <code>false</code> otherwise.
     */
    public static boolean isLoopbackAddress(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return (addr != null) && addr.isLoopbackAddress();
        } catch (UnknownHostException e) {
            log.info("Cannot check '" + ip + "'.", e);
            return false;
        }
    }

    public static boolean isLocalRequest(ServletRequest request) {
        String remote = request.getRemoteAddr();
        // TODO: think about proper implementation
        boolean b = isLoopbackAddress(remote);// ||
        // request.getLocalAddr().equals(remote);
        return b;
    }
}
