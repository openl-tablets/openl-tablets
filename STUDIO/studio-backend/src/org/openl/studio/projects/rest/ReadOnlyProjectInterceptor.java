package org.openl.studio.projects.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.dependencies.service.MavenCoordinates;
import org.openl.studio.projects.rest.annotations.ChangesNothing;

/**
 * Keeps a project downloaded from the Maven repository from being changed through the REST API.
 *
 * <p>Such a project is addressed by its coordinates, {@code maven:groupId:artifactId:version}, and every request about
 * it but a read is refused with {@code 409} {@code project.read-only.message}, whatever the permissions of the user. A
 * request that changes nothing though it is no {@code GET} — a compilation, a run, a test, a trace — is a read too: its
 * endpoint says so with {@link ChangesNothing}.
 *
 * @author Yury Molchan
 */
@Component
public class ReadOnlyProjectInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var projectId = ProjectRequests.projectIdOf(request);
        var coordinates = projectId == null ? null : MavenCoordinates.ofProjectId(projectId);
        if (coordinates != null && ProjectRequests.mayChange(request, handler)) {
            throw new ConflictException("project.read-only.message", coordinates.toString());
        }
        return true;
    }
}
