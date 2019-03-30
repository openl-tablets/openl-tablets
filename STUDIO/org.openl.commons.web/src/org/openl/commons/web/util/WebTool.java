package org.openl.commons.web.util;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public final class WebTool {
    private WebTool() {
    }

    public static String listRequestParams(ServletRequest request) {
        return listRequestParams(request.getParameterMap(), null);
    }

    public static String listRequestParams(ServletRequest request, String[] exceptParams) {
        return listRequestParams(request.getParameterMap(), exceptParams);
    }

    public static String listRequestParams(Map<String, String[]> paramsMap, String[] exceptParams) {
        StringBuilder buf = new StringBuilder();

        for (Map.Entry<String, String[]> entry : paramsMap.entrySet()) {
            String paramName = entry.getKey();
            if (ArrayUtils.contains(exceptParams, paramName)) {
                continue;
            }
            if (buf.length() != 0) {
                buf.append('&');
            }
            String[] paramValues = entry.getValue();
            buf.append(paramName).append('=').append(StringTool.encodeURL(paramValues[0]));
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
     * @return <code>true</code> if <code>ip</code> represents loopback address, or <code>false</code> otherwise.
     */
    public static boolean isLoopbackAddress(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return (addr != null) && addr.isLoopbackAddress();
        } catch (UnknownHostException e) {
            Logger log = LoggerFactory.getLogger(WebTool.class);
            log.info("Cannot check '{}'.", ip, e);
            return false;
        }
    }

    /**
     * Set content disposition HTTP header with a given file name. To support non-ASCII symbols in filenames we must use
     * RFC 2231 But old browsers and Safari still doesn't support it, so we mix old and new approach
     *
     * @param response servlet response
     * @param fileName file name that can contain non-ascii symbols
     * @see <a href="http://www.ietf.org/rfc/rfc2231.txt">RFC 2231</a>
     * @see <a href=
     *      "http://kbyanc.blogspot.com/2010/07/serving-file-downloads-with-non-ascii.html">serving-file-downloads-with-non-ascii</a>
     * @see <a href=
     *      "http://stackoverflow.com/questions/93551/how-to-encode-the-filename-parameter-of-content-disposition-header-in-http/6745788#6745788">Stackoverflow
     *      post</a>
     */
    public static void setContentDisposition(HttpServletResponse response, String fileName) {
        String encodedfileName = StringTool.encodeURL(fileName);
        response.setHeader("Content-Disposition",
            "attachment; filename=" + encodedfileName + "; filename*=UTF-8''" + encodedfileName);
    }

}
