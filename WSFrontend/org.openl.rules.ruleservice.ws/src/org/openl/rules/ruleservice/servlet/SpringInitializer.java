package org.openl.rules.ruleservice.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.openl.rules.openapi.OpenAPIConfiguration;
import org.openl.spring.env.PropertySourcesLoader;
import org.openl.util.ClassUtils;

@WebListener
public final class SpringInitializer implements ServletContextListener {

    private static final String THIS = SpringInitializer.class.getName();
    private ClassPathXmlApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext(ServletContext sc) {
        return ((SpringInitializer) sc.getAttribute(THIS)).applicationContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        OpenAPIConfiguration.configure();
        ServletContext servletContext = sce.getServletContext();
        servletContext.log("Initializing Spring root ApplicationContext");
        applicationContext = new ClassPathXmlApplicationContext();
        applicationContext.setId("OpenL_RuleService");

        // If not define classloader at this time, then CXF bus will get random classloader from the current thread
        // because of CXF bus can be initialized much later lazily. And at that time the classloader in the thread can
        // be set in one of instances of org.openl.classloader.OpenLBundleClassLoader
        // So to prevent it we set current classloader which is usually a Web application root class loader.
        // We don't use classloader of this class because of this class can be packaged outside of the application.
        // e.g. as a common dependency for the Spring Boot application.
        applicationContext.setClassLoader(ClassUtils.getCurrentClassLoader(getClass()));

        applicationContext.setConfigLocations("classpath:openl-ruleservice-ws-beans.xml");
        new PropertySourcesLoader().initialize(applicationContext, servletContext);
        applicationContext.refresh();
        applicationContext.registerShutdownHook();
        servletContext.setAttribute(THIS, this);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.removeAttribute(THIS);
        applicationContext.close();
    }

}
