package org.openl.rules.webstudio.web.servlet;

import java.io.File;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ClassPathConfigLocator;
import org.openl.config.ConfigLocator;
import org.openl.config.ConfigManager;
import org.openl.config.SysConfigManager;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Startup listener.
 *
 * @author Aleh Bykhavets
 *
 */
public class StartupListener implements ServletContextListener {

    private static final Log LOG = LogFactory.getLog(StartupListener.class);

    private static final String PROP_WEBSTUDIO_HOME = "webstudio.home";

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
        LOG.info("Starting " + name + "...");

        ConfigManager configManager = new ConfigManager();

        ClassPathConfigLocator defLocator = new ClassPathConfigLocator();
        defLocator.setPriority(0);

        WebConfigLocator webLocator = new WebConfigLocator(context);
        webLocator.setPriority(100);

        configManager.addLocator(webLocator);
        configManager.addLocator(defLocator);

        // Replace system config manager
        SysConfigManager.setConfigManager(configManager);

        initHomeDirectory(context);
    }


    private void initHomeDirectory(ServletContext context) {
        String webstudioHome = System.getProperty(PROP_WEBSTUDIO_HOME);
        if (webstudioHome == null) {
            // Set default value
            webstudioHome = context.getInitParameter(PROP_WEBSTUDIO_HOME);
            System.setProperty(PROP_WEBSTUDIO_HOME, webstudioHome);
        }
        File webstudioHomeDir = new File(
                StringUtils.defaultString(webstudioHome));
        if (!webstudioHomeDir.exists()) {
            LOG.fatal("You did not set up correctly webstudio.home variable: " + webstudioHome);
            return;
        }
        LOG.info(context.getServletContextName() + " home: " + webstudioHome);
    }

    public void contextDestroyed(ServletContextEvent event) {
        try {
            ProductionRepositoryFactoryProxy.release();
        } catch (RRepositoryException e) {
            LOG.error("Failed to release production repository", e);
        }
        try {
            RulesRepositoryFactory.release();
        } catch (RRepositoryException e) {
            LOG.error("Failed to release rules repository", e);
        }
        String name = event.getServletContext().getServletContextName();
        LOG.info(name + " is down.");
    }

}
