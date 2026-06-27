package org.openl.rules.webstudio.web.tab;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Establishes the request's {@link TabContext} for the legacy per-tab REST endpoints (table compile status and
 * message stacktraces) that run on the DispatcherServlet rather than the JSF lifecycle.
 *
 * <p>Registered only for those endpoints (not for project selection or the modern per-project REST API), so it
 * never overrides paths that already resolve a specific project. Requests without tab identity leave no context
 * and fall back to the session-global selection.
 */
@Component
public class TabContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TabContext context = TabContextResolver.resolve(request::getParameter);
        if (context != null) {
            TabContextHolder.set(context);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TabContextHolder.clear();
    }
}
