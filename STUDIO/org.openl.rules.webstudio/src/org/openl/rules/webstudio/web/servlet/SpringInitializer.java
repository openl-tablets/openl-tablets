package org.openl.rules.webstudio.web.servlet;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;

import org.openl.rules.webstudio.Migrator;
import org.openl.rules.webstudio.web.Props;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.spring.env.PropertySourcesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

@WebListener
public final class SpringInitializer implements Runnable, ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(SpringInitializer.class);
    private static final String THIS = SpringInitializer.class.getName();
    private static final int PERIOD = 10;

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock read = rwl.readLock();
    private final Lock write = rwl.writeLock();
    private final Map<String, HttpSession> cache = new ConcurrentHashMap<>();

    private XmlWebApplicationContext applicationContext;
    private ScheduledExecutorService scheduledPool;
    private ScheduledFuture<?> scheduled;

    public static ApplicationContext getApplicationContext(ServletContext sc) {
        return ((SpringInitializer) sc.getAttribute(THIS)).applicationContext;
    }

    public static void addSessionCache(HttpSession session) {
        ((SpringInitializer) session.getServletContext().getAttribute(THIS)).cache.put(session.getId(), session);
    }

    public static void removeSessionCache(HttpSession session) {
        ((SpringInitializer) session.getServletContext().getAttribute(THIS)).cache.remove(session.getId());
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

        // Register a WEB context path for easy access from Spring beans
        applicationContext.addBeanFactoryPostProcessor(
                bf -> bf.registerSingleton("servletContextPath", servletContext.getContextPath()));

        // Register Utility 'Props' class
        Props.setEnvironment(applicationContext.getEnvironment());
        applicationContext.addBeanFactoryPostProcessor(bf -> bf.registerSingleton("props", new Props()));

        // Do migrate before Spring initialization
        Migrator.migrate();

        applicationContext.refresh();

        // Store Spring context object for accessing from code.
        servletContext.setAttribute(THIS, this);
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

        startTimer();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.removeAttribute(THIS);
        servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        applicationContext.close();
        releaseTimer();
    }

    private void startTimer() {
        scheduledPool = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        scheduled = scheduledPool.scheduleWithFixedDelay(this, 1, PERIOD, TimeUnit.SECONDS);
    }

    private void releaseTimer() {
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
        try {
            if (DynamicPropertySource.get().reloadIfModified()) {
                refreshContext();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static Lock getLock(ServletContext sc) {
        return ((SpringInitializer) sc.getAttribute(THIS)).read;
    }

    // Trigger refresh context in the separate thread
    public static void refresh(ServletContext sc) {
        SpringInitializer springInitializer = ((SpringInitializer) sc.getAttribute(THIS));
        springInitializer.run();
    }

    private void refreshContext() {
        write.lock();
        try {
            applicationContext.refresh();
            for (HttpSession session : new ArrayList<>(cache.values())) {
                session.invalidate();
            }
        } finally {
            write.unlock();
        }
    }

}
