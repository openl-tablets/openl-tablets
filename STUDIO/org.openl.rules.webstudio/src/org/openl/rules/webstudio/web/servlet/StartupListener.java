package org.openl.rules.webstudio.web.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.openl.rules.webstudio.util.PreferencesManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup listener.
 *
 * @author Aleh Bykhavets
 */
public class StartupListener implements ServletContextListener {

    private final Logger log = LoggerFactory.getLogger(StartupListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String name = context.getServletContextName();
        log.info("Starting {}...", name);

        PreferencesManager.INSTANCE.initWebStudioMode(WebStudioUtils.getApplicationName(context));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        String name = event.getServletContext().getServletContextName();
        log.info("{} is down.", name);
    }

}
