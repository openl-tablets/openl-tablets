package org.openl.rules.webstudio.web.servlet;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.openl.config.ConfigurationManager;
import org.openl.util.StringUtils;
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
        ConfigurationManager cm = new ConfigurationManager("config.properties");
        Map<String, Object> properties = cm.getProperties(true);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            System.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
        }

        boolean configured = System.getProperty("webstudio.configured") != null && System
            .getProperty("webstudio.configured")
            .equals("true");
        // If webstudio.mode isn't defined, use either webstudio-beans.xml or installer-beans.xml.
        // If webstudio.mode is defined (for example "custom"), use specified custom-beans.xml spring configuration.
        String webStudioMode = System.getProperty("webstudio.mode");
        if (webStudioMode == null) {
            System.setProperty("webstudio.mode", configured ? "webstudio" : "installer");
        }

        // When WebStudio is configured we can set user mode to load appropriate Spring configuration.
        // If WebStudio isn't configured we must not set user mode globally. Instead the property must be loaded
        // directly
        // from property files and then redefine property if needed (in Install Wizard for example). It'll be set
        // globally
        // later when configuration will be finished.
        if (configured) {
            ConfigurationManager systemConfig = new ConfigurationManager(
                System.getProperty("webstudio.home") + "/system-settings/system.properties",
                "system.properties");
            String userMode = systemConfig.getStringProperty("user.mode");
            System.setProperty("user.mode", userMode);

            String repoPassKey = StringUtils
                .trimToEmpty(systemConfig.getStringProperty(ConfigurationManager.REPO_PASS_KEY));
            // Make it globally available. It will not be changed during application execution.
            System.setProperty(ConfigurationManager.REPO_PASS_KEY, repoPassKey);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        String name = event.getServletContext().getServletContextName();
        log.info("{} is down.", name);
    }

}
