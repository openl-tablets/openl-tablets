package org.openl.rules.webstudio.web.servlet;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class UserModeProfileActivator implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // At this time StartupListener.contextInitialized() is invoked already,
        // and "user.mode" system property is initialized with a not null value
        
        // Profile will be one of: singleUser, multiUser, ssoUser
        String profile = System.getProperty("user.mode") + "User";
        applicationContext.getEnvironment().addActiveProfile(profile);
    }

}
