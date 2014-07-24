package org.openl.rules.ruleservice.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ForwardingFilter implements Filter {
    private String forwardTo;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        forwardTo = filterConfig.getInitParameter("forwardTo");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher(forwardTo);
        dispatcher.forward(request, response);
    }

    @Override
    public void destroy() {

    }
}
