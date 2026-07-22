package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * Exposes startup and readiness health checks for OpenL Studio.
 *
 * <p>The startup endpoint returns {@code UP} when the web application can serve requests.
 *
 * <p>The readiness endpoint returns {@code READY} after Spring initialization. It returns HTTP 503 before
 * initialization and while the Spring context is being refreshed.
 *
 * @author Yury Molchan
 */
@Slf4j
@WebServlet(
        name = "healthCheckServlet",
        urlPatterns = {"/healthcheck/startup", "/healthcheck/readiness"},
        loadOnStartup = 1)
public final class HealthCheckServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        if (request.getServletPath().endsWith("/readiness")
                && !SpringInitializer.isReady(request.getServletContext())) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().write(request.getServletPath().endsWith("/startup") ? "UP" : "READY");
        } catch (IOException e) {
            log.warn("Failed to write the health check response.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
