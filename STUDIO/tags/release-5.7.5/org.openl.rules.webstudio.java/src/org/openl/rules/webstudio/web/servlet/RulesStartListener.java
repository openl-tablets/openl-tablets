package org.openl.rules.webstudio.web.servlet;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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
 * Updates system config manager.
 *
 * @author Aleh Bykhavets
 *
 */
public class RulesStartListener implements ServletContextListener {
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

    private static final Log LOG = LogFactory.getLog(RulesStartListener.class);

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

    public void contextInitialized(ServletContextEvent event) {
        String name = event.getServletContext().getServletContextName();
        LOG.info("Starting " + name + "...");

        ConfigManager configManager = new ConfigManager();

        ClassPathConfigLocator defLocator = new ClassPathConfigLocator();
        defLocator.setPriority(0);

        WebConfigLocator webLocator = new WebConfigLocator(event.getServletContext());
        webLocator.setPriority(100);

        configManager.addLocator(webLocator);
        configManager.addLocator(defLocator);

        // replace system config manager
        SysConfigManager.setConfigManager(configManager);
    }
}
