package org.openl.rules.webstudio.web.servlet;

import org.openl.config.ConfigurationManager;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class UserModeProfileActivator implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // At this time StartupListener.contextInitialized() is invoked already,
        // and "user.mode" system property is initialized with a not null value

        boolean configured = System.getProperty("webstudio.configured") != null
                && System.getProperty("webstudio.configured").equals("true");

        if (configured) {
            String userMode = new ConfigurationManager(true, System.getProperty("webstudio.home") + "/system-settings/system.properties",
                    System.getProperty("webapp.root") + "/WEB-INF/conf/system.properties").getStringProperty("user.mode");

            ((XmlWebApplicationContext) applicationContext).setConfigLocations(new String[] {
                "/WEB-INF/spring/webstudio-beans.xml",
                "/WEB-INF/spring/system-config-beans.xml",
                "/WEB-INF/spring/repository-beans.xml",
                "/WEB-INF/spring/security-beans.xml",
                "/WEB-INF/spring/security/security-" + userMode + ".xml"
            });
        }
    }

}
