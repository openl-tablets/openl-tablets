package org.openl.studio.security;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public final class RequestMatchers {

    private RequestMatchers() {
        // Utility class, no instantiation
    }

    public static RequestMatcher anyOf(String... patterns) {
        if (patterns == null || patterns.length == 0) {
            throw new IllegalArgumentException("Patterns must not be null or empty");
        }
        List<RequestMatcher> matchers = Stream.of(patterns)
                .map(RequestMatchers::matcher)
                .collect(Collectors.toList());
        if (matchers.size() == 1) {
            return matchers.getFirst();
        }
        return new OrRequestMatcher(matchers);
    }

    public static RequestMatcher matcher(String pattern) {
        return new AntPathRequestMatcher(pattern);
    }

    public static RequestMatcher not(RequestMatcher matcher) {
        return new NegatedRequestMatcher(matcher);
    }

}
