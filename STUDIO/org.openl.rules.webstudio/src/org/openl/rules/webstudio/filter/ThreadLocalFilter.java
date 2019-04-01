package org.openl.rules.webstudio.filter;

import java.io.IOException;

import javax.servlet.*;

/**
 * This filter is intended to prevent a memory leak. Server can use thread pool internally so a request thread can stay
 * alive upon end of the request.
 *
 * @author NSamatov
 */
public class ThreadLocalFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            // clear thread local variables
        }
    }

    @Override
    public void destroy() {
    }

}
