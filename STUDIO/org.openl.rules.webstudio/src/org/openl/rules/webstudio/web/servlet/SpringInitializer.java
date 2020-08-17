package org.openl.rules.webstudio.web.servlet;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.spring.env.PropertySourcesLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

@WebListener
public final class SpringInitializer implements Runnable, ServletContextListener {

    private static final String THIS = SpringInitializer.class.getName();
    public XmlWebApplicationContext applicationContext;

    private ScheduledExecutorService scheduledPool;
    private ScheduledFuture<?> scheduled;
    private final int PERIOD = 10;

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
        startMonitor();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.removeAttribute(THIS);
        applicationContext.close();
    }

    private void startMonitor() {
        scheduledPool = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        scheduled = scheduledPool.scheduleWithFixedDelay(this, 1, PERIOD, TimeUnit.SECONDS);
    }

    public synchronized void release() {
        if (scheduledPool != null) {
            scheduledPool.shutdownNow();
        }
        if (scheduled != null) {
            scheduled.cancel(true);
            scheduled = null;
        }
    }

    @Override
    public void run() {
        if (DynamicPropertySource.get().isPropWasModified()) {
            ReloadableDelegatingFilter.scheduleReload(applicationContext.getServletContext());
        }
    }
}
