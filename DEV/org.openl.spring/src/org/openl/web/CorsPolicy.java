package org.openl.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.core.env.PropertyResolver;

import org.openl.util.StringUtils;

/**
 * The CORS answer the {@code cors.*} settings prescribe, given alike by OpenL Studio and OpenL Rule Services.
 *
 * <p>A response to a listed origin names that origin in {@code Access-Control-Allow-Origin}. A pre-flight — an
 * {@code OPTIONS} request that names the method it asks about — is answered with {@code 204} and the allowed
 * methods, headers and cache time, and never travels on to the application. Any other response to a listed origin
 * names the response headers a page may read. Whenever an origin is listed, every response carries
 * {@code Vary: Origin}, so a shared cache never serves one origin's answer to another.
 *
 * <p>Credentials are never exposed; a cross-origin client authenticates with the {@code Authorization} header. A
 * blank method or header list and a missing or negative cache time leave their header out.
 *
 * @param allowedOrigins the origins that receive the headers
 * @param allowedMethods the methods a pre-flight answer allows
 * @param allowedHeaders the request headers a pre-flight answer allows
 * @param exposedHeaders the response headers a page of a listed origin may read
 * @param maxAge         the seconds a browser may cache a pre-flight answer
 * @author Yury Molchan
 */
public record CorsPolicy(AllowedOrigins allowedOrigins,
                         @Nullable String allowedMethods,
                         @Nullable String allowedHeaders,
                         @Nullable String exposedHeaders,
                         @Nullable Long maxAge) {

    public CorsPolicy {
        allowedMethods = StringUtils.trimToNull(allowedMethods);
        allowedHeaders = StringUtils.trimToNull(allowedHeaders);
        exposedHeaders = StringUtils.trimToNull(exposedHeaders);
        maxAge = maxAge == null || maxAge < 0 ? null : maxAge;
    }

    /**
     * Reads the {@code cors.allowed.origins}, {@code cors.allowed.methods}, {@code cors.allowed.headers},
     * {@code cors.exposed.headers} and {@code cors.preflight.maxage} settings.
     */
    public static CorsPolicy read(PropertyResolver settings) {
        return new CorsPolicy(AllowedOrigins.parse(settings.getProperty("cors.allowed.origins")),
                settings.getProperty("cors.allowed.methods"),
                settings.getProperty("cors.allowed.headers"),
                settings.getProperty("cors.exposed.headers"),
                settings.getProperty("cors.preflight.maxage", Long.class));
    }

    /**
     * Adds the CORS headers the request earns and answers a pre-flight in full. Returns whether the response is
     * complete: a pre-flight never travels on to the application.
     */
    public boolean handle(HttpServletRequest request, HttpServletResponse response) {
        if (!allowedOrigins.isEmpty()) {
            response.addHeader("Vary", "Origin");
        }
        var origin = request.getHeader("Origin");
        if (!allowedOrigins.contains(origin)) {
            return false;
        }
        response.setHeader("Access-Control-Allow-Origin", origin);
        if (!isPreflight(request)) {
            setIfPresent(response, "Access-Control-Expose-Headers", exposedHeaders);
            return false;
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        setIfPresent(response, "Access-Control-Allow-Methods", allowedMethods);
        setIfPresent(response, "Access-Control-Allow-Headers", allowedHeaders);
        setIfPresent(response, "Access-Control-Max-Age", maxAge == null ? null : Long.toString(maxAge));
        return true;
    }

    /** A pre-flight is an {@code OPTIONS} request that names the method it asks permission for. */
    private static boolean isPreflight(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod()) && request.getHeader("Access-Control-Request-Method") != null;
    }

    private static void setIfPresent(HttpServletResponse response, String name, @Nullable String value) {
        if (value != null) {
            response.setHeader(name, value);
        }
    }
}
