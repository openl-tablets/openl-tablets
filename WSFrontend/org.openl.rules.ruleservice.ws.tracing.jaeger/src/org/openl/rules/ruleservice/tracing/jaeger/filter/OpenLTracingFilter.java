package org.openl.rules.ruleservice.tracing.jaeger.filter;

import java.util.regex.Pattern;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.ruleservice.servlet.SpringInitializer;
import org.springframework.core.env.Environment;

import io.opentracing.contrib.web.servlet.filter.TracingFilter;

@WebFilter("/*")
public class OpenLTracingFilter extends TracingFilter {

    private Pattern skipPattern;

    private static final String DEFAULT_SKIP_URL = "/admin/[^ ]*";

    @Override
    public void init(FilterConfig config) throws ServletException {
        Environment env = SpringInitializer.getApplicationContext(config.getServletContext()).getEnvironment();
        String skipUrlsProperty = env.getProperty("ruleservice.tracing.skip.urls");
        StringBuilder skipUrls = new StringBuilder(DEFAULT_SKIP_URL);
        if (skipUrlsProperty != null) {
            skipUrls.append("|").append(skipUrlsProperty);
        }
        skipPattern = Pattern.compile(skipUrls.toString());
        super.init(config);
    }

    @Override
    protected boolean isTraced(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        boolean result = true;
        if (skipPattern != null) {
            int contextLength = httpRequest.getContextPath() == null ? 0 : httpRequest.getContextPath().length();
            String url = httpRequest.getRequestURI().substring(contextLength);
            result = !skipPattern.matcher(url).matches();
        }
        return result;
    }
}
