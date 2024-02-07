package org.openl.rules.webstudio.web.jsf;

public final class WebContext {
    private WebContext() {
    }

    private static String contextPath;

    /**
     * @return the contextPath
     */
    public static String getContextPath() {
        return contextPath;
    }

    /**
     * @param contextPath the contextPath to set
     */
    public static void setContextPath(String contextPath) {
        WebContext.contextPath = contextPath;
    }

}
