package org.openl.rules.webstudio.web.servlet;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import org.openl.info.OpenLInfoLogger;
import org.openl.rules.openapi.OpenAPIConfiguration;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.webstudio.Migrator;
import org.openl.rules.webstudio.web.Props;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.spring.env.PropertySourcesLoader;

@WebListener
@Slf4j
public final class SpringInitializer implements Runnable, ServletContextListener {

    private static final String THIS = SpringInitializer.class.getName();
    private static final int PERIOD = 10;
    // Keep short form fields in memory while spilling larger workbook parts to disk.
    private static final int MULTIPART_FILE_SIZE_THRESHOLD = 8 * 1024;

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock read = rwl.readLock();
    private final Lock write = rwl.writeLock();
    private final Map<String, HttpSession> cache = new ConcurrentHashMap<>();

    private XmlWebApplicationContext applicationContext;
    private ScheduledExecutorService scheduledPool;
    private ScheduledFuture<?> scheduled;
    private volatile boolean shuttingDown;

    public static @NonNull ApplicationContext getApplicationContext(@NonNull ServletContext sc) {
        return ((@NonNull SpringInitializer) sc.getAttribute(THIS)).applicationContext;
    }

    /**
     * Returns whether the Spring application context is active and available for requests.
     *
     * <p>The application is not ready before initialization completes, while the context is being refreshed, or
     * after shutdown begins.
     *
     * @param sc servlet context
     * @return {@code true} when OpenL Studio is ready to handle requests
     */
    static boolean isReady(@NonNull ServletContext sc) {
        @Nullable Object attribute = sc.getAttribute(THIS);
        if (!(attribute instanceof SpringInitializer initializer) || !initializer.read.tryLock()) {
            return false;
        }
        try {
            return !initializer.shuttingDown
                    && initializer.applicationContext != null
                    && initializer.applicationContext.isActive();
        } finally {
            initializer.read.unlock();
        }
    }

    public static void addSessionCache(HttpSession session) {
        ((SpringInitializer) session.getServletContext().getAttribute(THIS)).cache.put(session.getId(), session);
    }

    public static void removeSessionCache(HttpSession session, String sessionID) {
        ((SpringInitializer) session.getServletContext().getAttribute(THIS)).cache.remove(sessionID);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Force to register H2 JDBC driver
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        OpenAPIConfiguration.configure();

        ServletContext servletContext = sce.getServletContext();
        servletContext.log("Initializing Spring root ApplicationContext");
        applicationContext = new XmlWebApplicationContext();
        registerDispatcherServlet(servletContext);
        applicationContext.setServletContext(servletContext);
        applicationContext.setId("OpenL_Studio");
        applicationContext.setConfigLocations("/WEB-INF/spring/webstudio.xml");

        // If not define classloader at this time, then CXF bus will get random classloader from the current thread
        // because of CXF bus can be initialized much later lazily. And at that time the classloader in the thread can
        // be set in one of instances of org.openl.classloader.OpenLBundleClassLoader
        // So to prevent it we set current classloader which is usually a Web application root class loader.
        // We don't use classloader of this class because of this class can be packaged outside of the application.
        // e.g. as a common dependency for the Spring Boot application.
        applicationContext.setClassLoader(Thread.currentThread().getContextClassLoader());

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
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

        // Run migration which require context to be initialized
        Migrator.migrateAfterContentInitialized(applicationContext);
        // Experimental settings
        HTMLRenderer.MAX_NUM_CELLS = Props.integer("experimental.MAX_NUM_CELLS");

        startTimer();
        // Store the initializer only after OpenL Studio is ready to handle requests.
        servletContext.setAttribute(THIS, this);
    }

    /**
     * Register Spring Dispatcher Servlet
     *
     * @param sc servlet context
     */
    private void registerDispatcherServlet(ServletContext sc) {
        var dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setApplicationContext(applicationContext);
        var eventListener = (ApplicationListener<ContextRefreshedEvent>) dispatcherServlet::onApplicationEvent;
        applicationContext.addApplicationListener(eventListener);

        var registration = sc.addServlet("springDispatcher", dispatcherServlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/rest/*", "/web/*");

        var multipartConfigElement = new MultipartConfigElement("", -1L, -1L, MULTIPART_FILE_SIZE_THRESHOLD);
        registration.setMultipartConfig(multipartConfigElement);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        shuttingDown = true;
        OpenLInfoLogger.memStat();
        ServletContext servletContext = sce.getServletContext();
        servletContext.removeAttribute(THIS);
        servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        applicationContext.close();
        // Release JGit's process-wide resources only after the context has closed every repository, so a repository
        // teardown cannot submit work to an already-terminated JGit worker.
        Git.shutdown();
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
            log.error(e.getMessage(), e);
        }
    }

    public static @NonNull Lock getLock(@NonNull ServletContext sc) {
        return ((@NonNull SpringInitializer) sc.getAttribute(THIS)).read;
    }

    // Trigger refresh context in the separate thread
    public static void refresh(ServletContext sc) {
        SpringInitializer springInitializer = ((SpringInitializer) sc.getAttribute(THIS));
        springInitializer.run();
    }

    private void refreshContext() {
        write.lock();
        try {
            log.info("Refreshing Spring context to reload application configuration...");
            applicationContext.refresh();
            log.info("Cleaning up HTTP Sessions...");
            for (HttpSession session : new ArrayList<>(cache.values())) {
                session.invalidate();
            }
            log.info("OpenL Studio has been reloaded.");
        } finally {
            write.unlock();
        }
    }

}
