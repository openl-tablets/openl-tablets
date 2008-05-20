package org.openl.rules.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.util.net.NetUtils;

/**
 * Acegi filter, that automatically logins local user if the following
 * conditions are satisfied:
 * 
 * <pre>
 * 1. autoLogin is set to true
 * 2. NetUtils.isLocalRequest(request) == true
 * 3. no other user is currently logged in
 * </pre>
 * 
 * @author Andrey Naumenko
 */
public class LocalUserFilter implements Filter {
    private static final Log log = LogFactory.getLog(LocalUserFilter.class);
    private UserDetailsService userDetailsService;
    private boolean autoLogin;

    /**
     * Does nothing - we rely on IoC lifecycle services instead.
     */
    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (autoLogin && NetUtils.isLocalRequest(request)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(SecurityUtils.LOCAL_USER_ID);
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

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }
}
