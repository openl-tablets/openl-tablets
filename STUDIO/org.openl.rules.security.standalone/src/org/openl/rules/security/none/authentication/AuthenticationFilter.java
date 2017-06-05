package org.openl.rules.security.none.authentication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.openl.rules.security.DefaultPrivileges;
import org.openl.rules.security.none.SimpleAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Aliaksandr Antonik.
 */
public class AuthenticationFilter implements Filter {

    private static Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    static {
        authorities.add(DefaultPrivileges.ALL);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
            ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            authentication = new SimpleAuthenticationToken(authorities, "DEFAULT");
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
    }

}
