package org.openl.studio.projects.rest;

import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.converter.ProjectIdentityConverter;
import org.openl.studio.projects.rest.annotations.ChangesNothing;
import org.openl.studio.projects.rest.controller.ProjectsMergeController;
import org.openl.studio.projects.service.merge.ProjectsMergeLauncher;

/**
 * Keeps a project read-only for a client while the client merges it, and brings the editor up to date once the merge
 * has ended.
 *
 * <p>A branch merge runs in the background for minutes, and its end reopens the project in the editor. A request
 * that changed the project meanwhile — an edit, a save, a branch switch, a close — would be lost by the reopen or
 * would take the merge onto a branch it was not validated for, so every request but a read is refused with {@code 409}
 * {@code project.merge.running.message} while the merge runs. A request that changes nothing though it is no
 * {@code GET} — a compilation, a run, a trace, a comparison — is a read too: its endpoint says so with
 * {@link ChangesNothing}. The endpoints of the merge itself stay open: they read its result, send it again or cancel
 * it.
 *
 * <p>Every request about a project of the client brings the editor up to date first when its merge has ended, so the
 * conflicts of that merge are kept before anything else reads or changes the project.
 */
@Component
@RequiredArgsConstructor
public class MergeInProgressInterceptor implements HandlerInterceptor {

    private static final String PROJECT_ID = "projectId";
    private static final Set<String> READS = Set.of("GET", "HEAD", "OPTIONS");

    private final ProjectsMergeLauncher mergeLauncher;
    private final ProjectIdentityConverter projectIdentityConverter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var identity = projectIdentityOf(request);
        if (identity != null) {
            mergeLauncher.settleFinished();
            if (!READS.contains(request.getMethod()) && !isLetThrough(handler)
                    && mergeLauncher.isRunning(() -> projectIdentityConverter.convert(identity))) {
                throw new ConflictException("project.merge.running.message");
            }
        }
        return true;
    }

    private static @Nullable String projectIdentityOf(HttpServletRequest request) {
        return request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> variables
                && variables.get(PROJECT_ID) instanceof String identity ? identity : null;
    }

    /** Whether the endpoint goes on during a merge: an endpoint of the merge itself, or one that changes nothing. */
    private static boolean isLetThrough(Object handler) {
        return handler instanceof HandlerMethod method
                && (method.getBeanType() == ProjectsMergeController.class
                || method.hasMethodAnnotation(ChangesNothing.class));
    }
}
