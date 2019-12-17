package org.openl.rules.webstudio.web.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.openl.rules.webstudio.util.PreferencesManager;
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

        initSystemProperties();
    }

    private void initSystemProperties() {
        boolean configured = PreferencesManager.INSTANCE.isAppConfigured();

        // If webstudio.mode is not defined, use either webstudio-beans.xml or installer-beans.xml.
        // If webstudio.mode is defined (for example "custom"), use specified custom-beans.xml spring configuration.
        String webStudioMode = System.getProperty("webstudio.mode");
        if (webStudioMode == null) {
            System.setProperty("webstudio.mode", configured ? "webstudio" : "installer");
        }

        String webStudioHomeDirProp = System.getProperty("webstudio.home");
        String webStudioHomeDirPref = PreferencesManager.INSTANCE.getWebStudioHomeDir();

        if (webStudioHomeDirProp == null && webStudioHomeDirPref == null) {
            String webStudioHome = System.getProperty("webstudio.home.default");
            PreferencesManager.INSTANCE.setWebStudioHomeDir(webStudioHome);
        } else {
            if (webStudioHomeDirProp == null) {
                System.setProperty("webstudio.home", webStudioHomeDirPref);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        String name = event.getServletContext().getServletContextName();
        log.info("{} is down.", name);
    }

}
