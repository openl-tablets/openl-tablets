package org.openl.rules.security;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.openl.commons.web.util.WebTool;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Security filter, that automatically logins local user if the following
 * conditions are satisfied:
 *
 * <pre>
 * 1. autoLogin is set to true
 * 2. WebTool.isLocalRequest(request) == true
 * 3. no other user is currently logged in
 * 4. the urls we come from is not in 'blacklist' defined by property
 * <code>
 * ignoredUrls
 * </code>
 * ,
 * it allows to ignore auto login when processing login/logout requests.
 * </pre>
 *
 * @author Andrey Naumenko
 */
public class LocalUserFilter implements Filter {

    private UserDetailsService userDetailsService;
    private boolean autoLogin;
    private String[] ignoredUrls = new String[0];

    /**
     * Does nothing - we rely on IoC lifecycle services instead.
     */
    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (autoLogin && WebTool.isLocalRequest(request)
                && SecurityContextHolder.getContext().getAuthentication() == null && !isIgnoredRequest(request)) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(AccessManager.LOCAL_USER_ID);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails
                    .getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    /**
     * Does nothing - we rely on IoC lifecycle services instead.
     *
     * @param ignored not used
     *
     * @throws ServletException
     */
    public void init(FilterConfig ignored) throws ServletException {
    }

    protected boolean isIgnoredRequest(ServletRequest req) {
        HttpServletRequest request = (HttpServletRequest) req;
        String uri = request.getRequestURI();

        int queryParamIndex = uri.indexOf('?');
        if (queryParamIndex > 0) {
            // strip everything from the first question mark
            uri = uri.substring(0, queryParamIndex);
        }

        for (String url : ignoredUrls) {
            if (uri.endsWith(request.getContextPath() + url)) {
                return true;
            }
        }
        return false;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public void setIgnoredUrls(String[] ignoredUrls) {
        this.ignoredUrls = ignoredUrls;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
}
