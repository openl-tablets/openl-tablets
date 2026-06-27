package org.openl.rules.webstudio.web.tab;

import java.util.Optional;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Request-scoped storage for the current request's {@link TabContext}.
 *
 * <p>Backed by Spring request attributes, which are bound for both JSF (FacesServlet) and REST
 * (DispatcherServlet) requests. Background threads have no bound request, so {@link #get()} returns empty there
 * and callers fall back to the session-global selection.
 */
public final class TabContextHolder {

    private static final String ATTRIBUTE = TabContext.class.getName();

    private TabContextHolder() {
    }

    /**
     * The tab context resolved for the current request, or empty when none was established (no request bound, or
     * the request carried no tab identity).
     */
    public static Optional<TabContext> get() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((TabContext) attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
    }

    public static void set(TabContext context) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(ATTRIBUTE, context, RequestAttributes.SCOPE_REQUEST);
        }
    }

    public static void clear() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.removeAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
