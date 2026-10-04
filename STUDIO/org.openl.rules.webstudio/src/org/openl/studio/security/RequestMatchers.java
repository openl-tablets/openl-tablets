package org.openl.studio.security;

import java.util.stream.Stream;

import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import org.openl.rules.webstudio.web.servlet.StudioDispatcherServlet;
import org.openl.rules.webstudio.web.servlet.UserGuidesServlet;

public final class RequestMatchers {

    private RequestMatchers() {
        // Utility class, no instantiation
    }

    public static RequestMatcher anyOf(String... patterns) {
        if (patterns == null || patterns.length == 0) {
            throw new IllegalArgumentException("Patterns must not be null or empty");
        }
        var matchers = Stream.of(patterns)
                .map(RequestMatchers::matcher)
                .toList();
        if (matchers.size() == 1) {
            return matchers.getFirst();
        }
        return new OrRequestMatcher(matchers);
    }

    /** The patterns of the addresses that answer a caller with data, not a page: the REST API and the handshake. */
    public static String[] apiPatterns() {
        return new String[]{StudioDispatcherServlet.REST_PATH + "/**", StudioDispatcherServlet.WEB_SOCKET_PATH};
    }

    /** Matches the addresses that answer a caller with data, not a page: the REST API and the WebSocket handshake. */
    public static RequestMatcher api() {
        return anyOf(apiPatterns());
    }

    /**
     * Matches a file of the user guides: an address below {@code /docs} whose last part ends with an extension.
     *
     * <p>Every other address below {@code /docs} is a page of a guide, which the application page draws. The query
     * is not part of the address, so it cannot turn a page into a file.
     */
    public static RequestMatcher userGuideFiles() {
        return request -> UserGuidesServlet.PATH.equals(request.getServletPath())
                && request.getPathInfo() != null
                && UserGuidesServlet.namesFile(request.getPathInfo());
    }

    public static RequestMatcher matcher(String pattern) {
        return PathPatternRequestMatcher.withDefaults().matcher(pattern);
    }

    public static RequestMatcher not(RequestMatcher matcher) {
        return new NegatedRequestMatcher(matcher);
    }

}
