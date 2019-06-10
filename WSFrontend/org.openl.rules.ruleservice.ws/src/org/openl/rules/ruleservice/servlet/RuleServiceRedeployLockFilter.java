package org.openl.rules.ruleservice.servlet;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import javax.servlet.*;

import org.openl.rules.ruleservice.core.RuleServiceRedeployLock;

public class RuleServiceRedeployLockFilter implements javax.servlet.Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
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
