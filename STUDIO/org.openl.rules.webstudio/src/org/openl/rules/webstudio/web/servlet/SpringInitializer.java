package org.openl.rules.webstudio.web.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.openl.spring.env.PropertySourcesLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

@WebListener
public final class SpringInitializer implements ServletContextListener {

    private static final String THIS = SpringInitializer.class.getName();
    private XmlWebApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext(ServletContext sc) {
        return ((SpringInitializer) sc.getAttribute(THIS)).applicationContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.log("Initializing Spring root ApplicationContext");
        applicationContext = new XmlWebApplicationContext();
        applicationContext.setServletContext(servletContext);
        applicationContext.setId("OpenL_WebStudio");
        applicationContext.setConfigLocations("/WEB-INF/spring/webstudio.xml");
        new PropertySourcesLoader().initialize(applicationContext, servletContext);
        applicationContext.addBeanFactoryPostProcessor(
            bf -> bf.registerSingleton("servletContextPath", servletContext.getContextPath()));
        applicationContext.refresh();
        servletContext.setAttribute(THIS, this);
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.removeAttribute(THIS);
        applicationContext.close();
    }
}
