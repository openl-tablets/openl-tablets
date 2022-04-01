package org.openl.rules.ruleservice.servlet;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.openl.util.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;

/**
 * A servlet filter that inserts generated id in MDC for each request.
 *
 * @author ybiruk
 */
@WebFilter(value = "/*")
public class MDCFilter implements Filter {

    private static final String REQUEST_ID_KEY = "requestID";
    private String requestIdHeaderKey;
    private DoMDCFilter doMDCFilter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Environment env = SpringInitializer.getApplicationContext(filterConfig.getServletContext()).getEnvironment();
        requestIdHeaderKey = env.getProperty("log.request-id.header");
        if (StringUtils.isEmpty(requestIdHeaderKey)) {
            doMDCFilter = (ServletRequest request, ServletResponse response, FilterChain chain) -> chain
                .doFilter(request, response);
        } else {
            doMDCFilter = (ServletRequest request, ServletResponse response, FilterChain chain) -> {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String requestId = httpRequest.getHeader(requestIdHeaderKey);
                if (StringUtils.isBlank(requestId)) {
                    requestId = UUID.randomUUID().toString();
                }
                MDC.put(REQUEST_ID_KEY, requestId);
                try {
                    chain.doFilter(request, response);
                } finally {
                    MDC.remove(REQUEST_ID_KEY);
                }
            };
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        doMDCFilter.doFilter(request, response, chain);
    }

    @Override
    public void destroy() {
    }

    interface DoMDCFilter {
        void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                           ServletException;
    }
}
