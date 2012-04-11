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

import org.openl.commons.web.util.WebTool;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.AccessManager;
import org.openl.rules.security.none.SimpleAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Aliaksandr Antonik.
 */
public class AuthenticationFilter implements Filter {

    private static Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    static {
        authorities.add(new GrantedAuthorityImpl(Privileges.ROLE_ADMIN));
    }

    /**
     * Composes user name from a <code>ServletRequest</code>. If the request
     * comes from local computer, that is
     * <code>WebTool.isLocalRequest(req)</code> returns <code>true</code>
     * than a special value denoting local user is returned, otherwise user name
     * is IP address the request came from.
     *
     * @param req servlet request object
     * @return user name
     */
    private static String nameFromRequest(ServletRequest req) {
        if (WebTool.isLocalRequest(req)) {
            return AccessManager.LOCAL_USER_ID;
        }

        return req.getRemoteAddr();
    }

    /**
     * Called by the web container to indicate to a filter that it is being
     * taken out of service. This method is only called once all threads within
     * the filter's doFilter method have exited or after a timeout period has
     * passed. After the web container calls this method, it will not call the
     * doFilter method again on this instance of the filter. <br>
     * <br>
     *
     * This method gives the filter an opportunity to clean up any resources
     * that are being held (for example, memory, file handles, threads) and make
     * sure that any persistent state is synchronized with the filter's current
     * state in memory.
     */
    public void destroy() {
    }

    /**
     * The <code>doFilter</code> method of the Filter is called by the
     * container each time a request/response pair is passed through the chain
     * due to a client request for a resource at the end of the chain. The
     * FilterChain passed in to this method allows the Filter to pass on the
     * request and response to the next entity in the chain.
     * <p>
     * A typical implementation of this method would follow the following
     * pattern:- <br>
     * 1. Examine the request<br>
     * 2. Optionally wrap the request object with a custom implementation to
     * filter content or headers for input filtering <br>
     * 3. Optionally wrap the response object with a custom implementation to
     * filter content or headers for output filtering <br>
     * 4. a) <strong>Either</strong> invoke the next entity in the chain using
     * the FilterChain object (<code>chain.doFilter()</code>), <br>
     * 4. b) <strong>or</strong> not pass on the request/response pair to the
     * next entity in the filter chain to block the request processing<br>
     * 5. Directly set headers on the response after invocation of the next
     * entity in ther filter chain.
     */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
            ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            authentication = new SimpleAuthenticationToken(authorities, nameFromRequest(req));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(req, resp);
    }

    /**
     * Called by the web container to indicate to a filter that it is being
     * placed into service. The servlet container calls the init method exactly
     * once after instantiating the filter. The init method must complete
     * successfully before the filter is asked to do any filtering work. <br>
     * <br>
     * The web container cannot place the filter into service if the init method
     * either<br>
     * 1.Throws a ServletException <br>
     * 2.Does not return within a time period defined by the web container
     */
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}
