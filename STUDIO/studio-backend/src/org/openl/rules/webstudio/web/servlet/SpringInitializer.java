package org.openl.rules.webstudio.web.servlet;

import java.util.ArrayList;
import java.util.List;
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

import org.openl.info.OpenLInfoLogger;
import org.openl.rules.openapi.OpenAPIConfiguration;
import org.openl.rules.webstudio.Migrator;
import org.openl.rules.webstudio.web.Props;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.spring.env.PropertySourcesLoader;
import org.openl.studio.audit.service.ApplicationAuditor;
import org.openl.studio.audit.service.AuditClient;
import org.openl.studio.audit.service.EnvironmentSettingsAuditor;
import org.openl.studio.audit.service.SignInAuditor;
import org.openl.studio.session.ClientSessions;
import org.openl.studio.session.CredentialClient;

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

    // The H2 driver ships inside the web application, where DriverManager may not discover it by itself.
    @SuppressWarnings("java:S4925")
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Force to register H2 JDBC driver
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        OpenAPIConfiguration.configure();

        var servletContext = sce.getServletContext();
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

        // Let the static Props helpers read the environment
        Props.setEnvironment(applicationContext.getEnvironment());

        // Do migrate before Spring initialization
        var fromVersion = Migrator.migrate();

        applicationContext.refresh();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

        // Run migration which require context to be initialized
        Migrator.migrateAfterContentInitialized(applicationContext, fromVersion);
        applicationContext.getBean(ApplicationAuditor.class).started();
        // The deployment sets its settings once for a run, so they are compared at the start, never at a refresh.
        applicationContext.getBean(EnvironmentSettingsAuditor.class).compareWithLastStart();
        startTimer();
        // Store the initializer only after OpenL Studio is ready to handle requests.
        servletContext.setAttribute(THIS, this);
    }

    /**
     * Registers the Spring dispatcher. It answers the REST API under {@code /rest} and the WebSocket handshake at
     * {@code /ws}.
     *
     * @param sc servlet context
     */
    private void registerDispatcherServlet(ServletContext sc) {
        var dispatcherServlet = new StudioDispatcherServlet();
        dispatcherServlet.setApplicationContext(applicationContext);
        var eventListener = (ApplicationListener<ContextRefreshedEvent>) dispatcherServlet::onApplicationEvent;
        applicationContext.addApplicationListener(eventListener);

        var registration = sc.addServlet("springDispatcher", dispatcherServlet);
        registration.setLoadOnStartup(1);
        registration.addMapping(StudioDispatcherServlet.REST_PATH + "/*", StudioDispatcherServlet.WEB_SOCKET_PATH);

        var multipartConfigElement = new MultipartConfigElement("", -1L, -1L, MULTIPART_FILE_SIZE_THRESHOLD);
        registration.setMultipartConfig(multipartConfigElement);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        shuttingDown = true;
        OpenLInfoLogger.memStat();
        var servletContext = sce.getServletContext();
        servletContext.removeAttribute(THIS);
        servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (applicationContext.isActive()) {
            applicationContext.getBean(ApplicationAuditor.class).stopped();
        }
        applicationContext.close();
        // Release JGit's process-wide resources only after the context has closed every repository, so a repository
        // teardown cannot submit work to an already-terminated JGit worker.
        Git.shutdown();
        releaseTimer();
    }

    private void startTimer() {
        scheduledPool = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = Executors.defaultThreadFactory().newThread(r);
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
        var springInitializer = (SpringInitializer) sc.getAttribute(THIS);
        springInitializer.run();
    }

    private void refreshContext() {
        write.lock();
        try {
            log.info("Refreshing Spring context to reload application configuration...");
            var clients = clientsToEnd();
            applicationContext.refresh();
            // OpenL Studio restarts itself, whichever request happened to find the settings changed.
            AuditClient.runAsApplication(() -> finishRestart(clients));
            log.info("OpenL Studio has been reloaded.");
        } finally {
            write.unlock();
        }
    }

    /**
     * The clients of credentials the refresh is about to end with the old context, listed before it while that context
     * runs. None when a refresh before failed and left the context inactive, so that this refresh can still bring
     * OpenL Studio back.
     */
    private List<CredentialClient> clientsToEnd() {
        return applicationContext.isActive()
                ? applicationContext.getBean(ClientSessions.class).clientsToEnd()
                : List.of();
    }

    /**
     * Records the restart and ends every HTTP session, so that each user signs in to the reloaded configuration. The
     * ends of the sessions of the clients of credentials the refresh ended are recorded too.
     */
    private void finishRestart(List<CredentialClient> clients) {
        applicationContext.getBean(ApplicationAuditor.class).restarted();
        log.info("Cleaning up HTTP Sessions...");
        new ArrayList<>(cache.values()).forEach(SpringInitializer::endSession);
        applicationContext.getBean(SignInAuditor.class).restarted(clients);
    }

    /**
     * Ends a session the restart leaves behind. A session that has just ended by itself is skipped: it ends only once.
     */
    private static void endSession(HttpSession session) {
        try {
            // The users did not sign out themselves, so the audit log records the end of their sessions.
            SignInAuditor.endedByRestart(session);
            session.invalidate();
        } catch (IllegalStateException e) {
            log.debug("The session '{}' has already ended.", session.getId());
        }
    }

}
