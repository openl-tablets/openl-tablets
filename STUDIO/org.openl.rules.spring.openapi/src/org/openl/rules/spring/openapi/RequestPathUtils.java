package org.openl.rules.spring.openapi;

import jakarta.servlet.http.HttpServletRequest;

import org.openl.util.StringUtils;

/**
 * Utility class for constructing request URLs from {@link HttpServletRequest}.
 *
 * @author Aliaksei Tsymbalist
 */
public class RequestPathUtils {

    public static String getFullRequestPath(HttpServletRequest request) {
        StringBuilder url = new StringBuilder(getRequestBasePath(request));
        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath)) {
            url.append(contextPath);
        }
        String servletPath = request.getServletPath();
        if (StringUtils.isNotBlank(servletPath)) {
            url.append(servletPath);
        }
        return url.toString();
    }

    public static String getRequestBasePath(HttpServletRequest request) {
        final var scheme = request.getScheme();
        final var port = request.getServerPort();
        final var url = new StringBuilder(scheme).append("://").append(request.getServerName());

        if ("http".equals(scheme) && port != 80 || "https".equals(scheme) && port != 443) {
            url.append(':').append(port);
        }

        return url.toString();
    }
}
