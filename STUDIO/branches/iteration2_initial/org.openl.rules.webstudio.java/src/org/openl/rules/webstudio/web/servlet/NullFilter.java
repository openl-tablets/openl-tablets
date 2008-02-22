package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * <p>Filter that does nothing. It can be useful as part of Acegi FilterChain.</p>
 *
 * @author Andrey Naumenko
 */
public class NullFilter implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException {}

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
        FilterChain filterChain) throws IOException, ServletException
    {
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {}
}
