package org.openl.rules.ruleservice.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.openl.rules.ruleservice.core.RuleServiceRedeployLock;

public class RuleServiceRedeployLockFilter implements javax.servlet.Filter {
    String[] ignorePrefixes;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String ignorePrefixesParameter = filterConfig.getInitParameter("ignorePrefixes");
        if (ignorePrefixesParameter != null) {
            ignorePrefixes = Arrays.stream(ignorePrefixesParameter.split(","))
                .map(String::trim)
                .toArray(size -> new String[size]);
        } else {
            ignorePrefixes = new String[] {};
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        for (String ignorePrefix : ignorePrefixes) {
            String path = ((HttpServletRequest) request).getPathInfo();
            if (path != null && path.startsWith(ignorePrefix)) {
                chain.doFilter(request, response);
                return;
            }
        }
        Lock lock = RuleServiceRedeployLock.getInstance().getReadLock();
        try {
            lock.lock();
            chain.doFilter(request, response);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void destroy() {

    }
}
