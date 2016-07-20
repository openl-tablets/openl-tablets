package org.openl.rules.webstudio.web.servlet;

import org.openl.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.InputStream;
import java.util.Map;

/**
 * Startup listener.
 *
 * @author Aleh Bykhavets
 */
public class StartupListener implements ServletContextListener {

    private final Logger log = LoggerFactory.getLogger(StartupListener.class);

    private static class WebConfigLocator extends ConfigLocator {
        private final ServletContext context;

        private WebConfigLocator(ServletContext context) {
            this.context = context;
        }

        @Override
        public InputStream locate(String fullName) {
            String resPath = "/WEB-INF/conf/";

            if (fullName.startsWith("/")) {
                resPath += fullName.substring(1);
            } else {
                resPath += fullName;
            }

            return context.getResourceAsStream(resPath);
        }

    }

    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String name = context.getServletContextName();
        log.info("Starting {}...", name);

        ConfigManager configManager = new ConfigManager();

        ClassPathConfigLocator defLocator = new ClassPathConfigLocator();
        defLocator.setPriority(0);

        WebConfigLocator webLocator = new WebConfigLocator(context);
        webLocator.setPriority(100);

        configManager.addLocator(webLocator);
        configManager.addLocator(defLocator);

        // Replace system config manager
        SysConfigManager.setConfigManager(configManager);

        initSystemProperties();
    }

    private void initSystemProperties() {
        ConfigurationManager cm = new ConfigurationManager(true,
                System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        Map<String, Object> properties = cm.getProperties(true);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            System.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
        }

        // If webstudio.mode isn't defined, use either webstudio-beans.xml or installer-beans.xml.
        // If webstudio.mode is defined (for example "custom"), use specified custom-beans.xml spring configuration.
        String webStudioMode = System.getProperty("webstudio.mode");
        if (webStudioMode == null) {
            boolean configured = System.getProperty("webstudio.configured") != null
                    && System.getProperty("webstudio.configured").equals("true");
            System.setProperty("webstudio.mode", configured ? "webstudio" : "installer");
        }

        String userMode = new ConfigurationManager(true, System.getProperty("webstudio.home") + "/system-settings/system.properties",
                System.getProperty("webapp.root") + "/WEB-INF/conf/system.properties").getStringProperty("user.mode");
        System.setProperty("user.mode", userMode);
    }

    public void contextDestroyed(ServletContextEvent event) {
        // Clear previous ConfigManager
        SysConfigManager.setConfigManager(new ConfigManager());
        String name = event.getServletContext().getServletContextName();
        log.info("{} is down.", name);
    }

}
