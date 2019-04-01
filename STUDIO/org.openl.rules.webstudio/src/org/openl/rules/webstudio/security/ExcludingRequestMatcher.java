package org.openl.rules.webstudio.security;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @author nsamatov.
 */
public class ExcludingRequestMatcher implements RequestMatcher {
    private List<RequestMatcher> matchers = new ArrayList<>();

    @Override
    public boolean matches(HttpServletRequest request) {
        for (RequestMatcher matcher : matchers) {
            if (matcher.matches(request)) {
                return false;
            }
        }
        return true;
    }

    public void setMatchers(List<RequestMatcher> matchers) {
        this.matchers = matchers;
    }
}
