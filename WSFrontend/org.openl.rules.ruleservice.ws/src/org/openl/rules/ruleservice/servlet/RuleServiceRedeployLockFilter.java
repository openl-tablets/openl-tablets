package org.openl.rules.ruleservice.servlet;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.openl.rules.ruleservice.core.RuleServiceRedeployLock;

/**
 * TODO: Replace locking of all requests by locking per OpenL service in RuleService
 */
@WebFilter("/*")
public class RuleServiceRedeployLockFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        String path = ((HttpServletRequest) request).getPathInfo();
        if (path != null && path.startsWith("/admin")) { // Do not block admin functionality
            chain.doFilter(request, response);
            return;
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
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
