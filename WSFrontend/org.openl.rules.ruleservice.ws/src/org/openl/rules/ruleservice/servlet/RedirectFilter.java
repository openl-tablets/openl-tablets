package org.openl.rules.ruleservice.servlet;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

public class RedirectFilter implements javax.servlet.Filter {

    private String redirectTo;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.redirectTo = filterConfig.getInitParameter("redirectTo");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (redirectTo.startsWith("/")) {
            httpResponse.sendRedirect(redirectTo);
        } else {
            httpResponse.sendRedirect(request.getServletContext().getContextPath() + "/" + redirectTo);
        }
    }

    @Override
    public void destroy() {

    }
}
