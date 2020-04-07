package org.openl.rules.ruleservice.servlet;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;

import org.openl.spring.env.PropertySourcesLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

@WebListener
public final class SpringInitializer extends ContextLoaderListener {

    public SpringInitializer() {
        setContextInitializers(new PropertySourcesLoader());
    }

    @Override
    protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
        XmlWebApplicationContext rootContext = new XmlWebApplicationContext();
        rootContext.setConfigLocations("classpath:openl-ruleservice-beans.xml",
            "classpath:openl-ruleservice-store-log-data-beans.xml",
            "classpath:openl-ruleservice-admin-beans.xml",
            "classpath:openl-ruleservice-override-beans.xml");
        return rootContext;
    }
}
