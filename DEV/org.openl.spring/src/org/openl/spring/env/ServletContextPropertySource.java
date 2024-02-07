package org.openl.spring.env;

import java.util.Collections;
import javax.servlet.ServletContext;

import org.springframework.core.env.EnumerablePropertySource;

import org.openl.util.StringUtils;

public class ServletContextPropertySource extends EnumerablePropertySource<ServletContext> {

    public ServletContextPropertySource(String name, ServletContext servletContext) {
        super(name, servletContext);
        ConfigLog.LOG.info("Loading ServletContext init parameters: {} properties.", getPropertyNames().length);
    }

    @Override
    public String[] getPropertyNames() {
        return Collections.list(source.getInitParameterNames()).toArray(StringUtils.EMPTY_STRING_ARRAY);
    }

    @Override
    public String getProperty(String name) {
        return this.source.getInitParameter(name);
    }

}