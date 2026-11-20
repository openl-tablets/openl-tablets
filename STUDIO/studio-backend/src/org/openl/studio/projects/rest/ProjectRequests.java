package org.openl.studio.projects.rest;

import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import org.openl.studio.projects.rest.annotations.ChangesNothing;

/**
 * What a request of the REST API tells about the project it is about.
 *
 * @author Yury Molchan
 */
final class ProjectRequests {

    private static final String PROJECT_ID = "projectId";
    private static final Set<String> READS = Set.of("GET", "HEAD", "OPTIONS");

    private ProjectRequests() {
    }

    /** The id of the project the address of the request names, or {@code null} when it names none. */
    static @Nullable String projectIdOf(HttpServletRequest request) {
        return request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> variables
                && variables.get(PROJECT_ID) instanceof String identity ? identity : null;
    }

    /**
     * Whether the request may change the project: it is no read, and its endpoint does not say with
     * {@link ChangesNothing} that it changes nothing, as a compilation, a run or a trace does.
     */
    static boolean mayChange(HttpServletRequest request, Object handler) {
        return !READS.contains(request.getMethod()) && !changesNothing(handler);
    }

    private static boolean changesNothing(Object handler) {
        return handler instanceof HandlerMethod method && method.hasMethodAnnotation(ChangesNothing.class);
    }
}
