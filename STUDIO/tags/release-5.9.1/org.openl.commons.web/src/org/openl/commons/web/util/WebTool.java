package org.openl.commons.web.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.util.StringTool;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import javax.servlet.ServletRequest;

/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public class WebTool {

    private static final Log LOG = LogFactory.getLog(WebTool.class);

    public static String listRequestParams(ServletRequest request) {
        return listRequestParams(request.getParameterMap(), null);
    }

    public static String listRequestParams(ServletRequest request, String[] exceptParams) {
        return listRequestParams(request.getParameterMap(), exceptParams);
    }

    public static String listRequestParams(Map<String, String[]> paramsMap, String[] exceptParams) {
        StringBuilder buf = new StringBuilder();

        for (String paramName : paramsMap.keySet()) {
            if (ArrayUtils.contains(exceptParams, paramName)) {
                continue;
            }
            if (buf.length() != 0) {
                buf.append('&');
            }
            String[] paramValues = paramsMap.get(paramName);
            buf.append(paramName).append('=').append(
                    StringTool.encodeURL(paramValues[0]));
        }

        return buf.toString();
    }

    public static boolean isLocalRequest(ServletRequest request) {
        String remote = request.getRemoteAddr();
        // TODO: think about proper implementation
        boolean b = isLoopbackAddress(remote);// ||
        // request.getLocalAddr().equals(remote);
        return b;
    }

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
            LOG.info("Cannot check '" + ip + "'.", e);
            return false;
        }
    }

}
