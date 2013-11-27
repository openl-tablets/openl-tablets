package org.openl.rules.webstudio.web.servlet;

import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ClassPathConfigLocator;
import org.openl.config.ConfigLocator;
import org.openl.config.ConfigManager;
import org.openl.config.ConfigurationManager;
import org.openl.config.SysConfigManager;

/**
 * Startup listener.
 *
 * @author Aleh Bykhavets
 *
 */
public class StartupListener implements ServletContextListener {

    private final Log log = LogFactory.getLog(StartupListener.class);

    private class WebConfigLocator extends ConfigLocator {
        private ServletContext context;

        WebConfigLocator(ServletContext context) {
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
        log.info("Starting " + name + "...");

        ConfigManager configManager = new ConfigManager();

        ClassPathConfigLocator defLocator = new ClassPathConfigLocator();
        defLocator.setPriority(0);

        WebConfigLocator webLocator = new WebConfigLocator(context);
        webLocator.setPriority(100);

        configManager.addLocator(webLocator);
        configManager.addLocator(defLocator);

        // Replace system config manager
        SysConfigManager.setConfigManager(configManager);

        initSystemProperties(context);
    }

    private void initSystemProperties(ServletContext context) {
        ConfigurationManager cm = new ConfigurationManager(true,
                System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        Map<String, Object> properties = cm.getProperties(true);

        for (String propertyName : properties.keySet()) {
            Object propValue = properties.get(propertyName);
            System.setProperty(propertyName, String.valueOf(propValue));
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        String name = event.getServletContext().getServletContextName();
        log.info(name + " is down.");
    }

}
