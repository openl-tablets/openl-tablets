package org.openl.spring.env;

import java.util.HashMap;
import jakarta.servlet.ServletContext;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiPropertySource;

/**
 * Allows to combine environment properties, default application properties and external application properties. This
 * class can be used in the following ways:
 * <ul>
 * <li>In the web.xml, using {@linkplain org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * "contextInitializerClasses"} context parameter.</li>
 * <li>In Spring java configuration like <code>new PropertySourcesLoader().initialize(applicationContext)</code></li>
 * </ul>
 * Resolving order (next resource overrides previous):
 * <li>OpenL default properties. {@link DefaultPropertySource}</li>
 * <li>Application externalized configuration. {@link ApplicationPropertySource} <br>
 * <li>Application modifiable configuration. {@link DynamicPropertySource} <br>
 * <li>OS environment variables. {@link System#getenv()}</li>
 * <li>Java System properties. {@link System#getProperties()}</li>
 * <li>JNDI attributes from {@code java:comp/env}</li>
 * <li>Servlet context loadProperties parameters from
 * {@link jakarta.servlet.ServletContext#getInitParameter(java.lang.String)}</li>
 * </ul>
 *
 * @author Yury Molchan
 * @see org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * @see org.springframework.beans.factory.config.PlaceholderConfigurerSupport
 * @see org.springframework.context.ApplicationContextInitializer
 * @see ApplicationPropertySource
 */
public class PropertySourcesLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext appContext) {
        ConfigLog.LOG
                .info("The initialization of properties from 'contextInitializerClasses' context-param in web.xml");
        var oldEnv = appContext.getEnvironment();
        var env = new FirewallEnvironment(oldEnv.getPropertySources());
        appContext.setEnvironment(env);
        String appName = normalizeAppName(appContext.getApplicationName());
        loadEnvironment(appContext, env, appName, null);
    }

    public void initialize(ConfigurableApplicationContext appContext, ServletContext servletContext) {
        ConfigLog.LOG.info("The initialization of properties");
        var env = new FirewallEnvironment(new MutablePropertySources());
        appContext.setEnvironment(env);
        String appName = normalizeAppName(servletContext.getContextPath());
        loadEnvironment(appContext, env, appName, servletContext);
    }

    private void loadEnvironment(ConfigurableApplicationContext appContext,
                                 FirewallEnvironment env,
                                 String appName,
                                 ServletContext servletContext) {
        var props = env.getRawPropertyResolver();
        var propertySources = env.getPropertySources();

        ConfigLog.LOG.info("Loading OpenL System Info properties...");
        propertySources.addFirst(new SysInfoPropertySource());

        if (servletContext != null) {
            propertySources.addLast(new ServletContextPropertySource("ServletContext init parameters", servletContext));
            // Assuming that there is org.springframework.core.env.StandardEnvironment
            if (JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()) {
                ConfigLog.LOG.info("Loading JNDI properties...");
                propertySources.addLast(new JndiPropertySource("JNDI properties"));
            }
        }
        propertySources.addLast(new SysPropPropertySource(env.getSystemProperties()));
        propertySources.addLast(new SysEnvRefPropertySource(env.getSystemEnvironment()));

        ConfigLog.LOG.info("Loading Random properties...");
        propertySources.addLast(new RandomValuePropertySource());

        ConfigLog.LOG.info("Loading default properties...");
        var defaultPropertySource = new DefaultPropertySource();
        propertySources.addLast(defaultPropertySource);

        ConfigLog.LOG.info("Loading application properties...");
        var profiles = env.getActiveProfiles();
        propertySources.addBefore(DefaultPropertySource.PROPS_NAME,
                new ApplicationPropertySource(props, appName, profiles));

        ConfigLog.LOG.info("Loading reconfigurable properties...");
        var propertySource = new DynamicPropertySource(appName, props);
        DynamicPropertySource.THE = propertySource;
        propertySources.addBefore(ApplicationPropertySource.PROPS_NAME, propertySource);

        propertySources.addBefore(DynamicPropertySource.PROPS_NAME, new DisablePropertySource(propertySources));

        ConfigLog.LOG.info("Register reference property processor...");
        propertySources.addLast(new RefPropertySource(props, propertySources));

        ConfigLog.LOG.info("Activating a firewall against insecure properties keys...");
        props.initFirewall();
        registerPropertyBean(appContext, defaultPropertySource, props);
        ConfigLog.LOG.info("Loading of the properties has been finished.");
    }

    private void registerPropertyBean(ConfigurableApplicationContext appContext,
                                      DefaultPropertySource defaultPropertySource,
                                      FirewallPropertyResolver props) {
        var propertyMap = new HashMap<String, String>();
        for (String key : defaultPropertySource.getPropertyNames()) {
            propertyMap.put(key, props.getRawProperty(key));
        }
        appContext.addBeanFactoryPostProcessor(bf -> bf.registerSingleton("PropertyBean",
                new PropertyBean(defaultPropertySource.getSource(), propertyMap)));
    }

    private static String normalizeAppName(String appName) {
        if (appName.isEmpty()) {
            return "";
        }
        return appName.replace('/', ' ').replace('\\', ' ').trim().replace(' ', '-');
    }

}
