package org.openl.spring.env;

import java.io.IOException;
import java.util.Properties;

import org.openl.util.CollectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.StringValueResolver;

/**
 * Replacement of {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer}. Allows to combine
 * environment properties, default application properties and external application properties. Application properties
 * can be get as combinations of locations and names. If a location is a folder then it is concatenated with each name.
 * For example:
 * <p>
 * application name (from the Spring application context): WebStudio<br>
 * Spring active profiles: prod, qa<br>
 * locations: file:, file:openl.properties, file:openl/<br>
 * names: application.properties, {appName}.properties, {profile}.properties<br>
 * <br>
 * Then the list of resources to search is (next resource overides previous):
 * <ol>
 * <li>file:foo.jar!/openl-default.properties</li>
 * <li>classpath:openl-default.properties</li>
 * <li>file:bar.jar!/openl-default.properties</li>
 * <li>file:application.properties</li>
 * <li>file:WebStudio.properties</li>
 * <li>file:prod.properties</li>
 * <li>file:qa.properties</li>
 * <li>file:openl.properties</li>
 * <li>file:openl/application.properties</li>
 * <li>file:openl/WebStudio.properties</li>
 * <li>file:openl/prod.properties</li>
 * <li>file:openl/qa.properties</li>
 * <li>Application context environment properties</li>
 * </ol>
 * </p>
 * This class can be used in the following ways:
 * <ul>
 * <li>In the web.xml, using {@linkplain org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * "contextInitializerClasses"} context parameter.</li>
 * <li>In Spring configuration, declaring {@code <bean class="org.openl.spring.env.PropertySourcesLoader"/>} bean.</li>
 * </ul>
 * Default resolving order (next resource overides previous):
 * <li>OpenL default properties.
 * <ol>
 * <li>classpath*:openl-default.properties</li>
 * </ol>
 * </li>
 * <li>Properties stored in a hierarchical collection of preference data using Preferences API. See
 * {@link java.util.prefs.Preferences Preferences} for details. Preferences will be stored in a node with a key:
 * <ol>
 * <li>openl/{appName}</li>
 * </ol>
 * </li>
 * <li>Application externalized configuration. <br>
 * Can be overridden using {@code openl.config.name} or {@code spring.config.name} properties for names. <br>
 * And {@code openl.config.location} or {@code spring.config.location} properties for folders and locations.
 * <ol>
 * <li>classpath:application*-default.properties</li>
 * <li>classpath:application.properties</li>
 * <li>classpath:application-{profile}.properties</li>
 * <li>classpath:{appName}.properties</li>
 * <li>classpath:{appName}-{profile}.properties</li>
 * <li>classpath:conf/application.properties</li>
 * <li>classpath:conf/application-{profile}.properties</li>
 * <li>classpath:conf/{appName}.properties</li>
 * <li>classpath:conf/{appName}-{profile}.properties</li>
 * <li>classpath:config/application.properties</li>
 * <li>classpath:config/application-{profile}.properties</li>
 * <li>classpath:config/{appName}.properties</li>
 * <li>classpath:config/{appName}-{profile}.properties</li>
 * <li>file:application.properties</li>
 * <li>file:application-{profile}.properties</li>
 * <li>file:{appName}.properties</li>
 * <li>file:{appName}-{profile}.properties</li>
 * <li>file:conf/application.properties</li>
 * <li>file:conf/application-{profile}.properties</li>
 * <li>file:conf/{appName}.properties</li>
 * <li>file:conf/{appName}-{profile}.properties</li>
 * <li>file:config/application.properties</li>
 * <li>file:config/application-{profile}.properties</li>
 * <li>file:config/{appName}.properties</li>
 * <li>file:config/{appName}-{profile}.properties</li>
 * <li>file:${user.home}/application.properties</li>
 * <li>file:${user.home}/application-{profile}.properties</li>
 * <li>file:${user.home}/{appName}.properties</li>
 * <li>file:${user.home}/{appName}-{profile}.properties</li>
 * <li>file:${user.home}/openl.properties</li>
 * <li>file:${openl.home}/application.properties</li>
 * <li>file:${openl.home}/application-{profile}.properties</li>
 * <li>file:${openl.home}/{appName}.properties</li>
 * <li>file:${openl.home}/{appName}-{profile}.properties</li>
 * </ol>
 * </li>
 * <li>Spring environment.
 * <ol>
 * <li>OS environment variables. {@link System#getenv()}</li>
 * <li>Java System properties. {@link System#getProperties()}</li>
 * <li>JNDI attributes from {@code java:comp/env}</li>
 * <li>Servlet context loadProperties parameters from
 * {@link javax.servlet.ServletContext#getInitParameter(java.lang.String)}</li>
 * <li>Servlet config loadProperties parameters from
 * {@link javax.servlet.ServletConfig#getInitParameter(java.lang.String)}</li>
 * </ol>
 * </li>
 * </ul>
 *
 * @author Yury Molchan
 * @see org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * @see org.springframework.beans.factory.config.PlaceholderConfigurerSupport
 * @see org.springframework.context.ApplicationContextInitializer
 * @see ApplicationPropertySources
 */
public class PropertySourcesLoader extends PlaceholderConfigurerSupport implements ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationContextAware {

    public static final String OPENL_ADDITIONAL_PROPS = "OpenL additional properties";
    public static final String ENVIRONMENT_PROPS = "environmentProps";

    private ApplicationContext appContext;
    private MutablePropertySources propertySources;

    {
        setIgnoreResourceNotFound(true);
    }

    @Override
    public void initialize(ConfigurableApplicationContext appContext) {
        ConfigLog.LOG
            .info("The initialization of properties from 'contextInitializerClasses' context-param in web.xml");
        doInitialize(appContext);

        // We need to reinitialize property sources when application context is refreshed because openl.home can be
        // changed in Install Wizard. We must do it before any bean is created, so we can't use ContextRefreshedEvent,
        // that's why we reinitialize settings when context is closed.
        appContext.addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> {
            ApplicationContext applicationContext = event.getApplicationContext();
            if (applicationContext instanceof ConfigurableApplicationContext) {
                doInitialize((ConfigurableApplicationContext) applicationContext);
            }
        });
    }

    private void doInitialize(ConfigurableApplicationContext appContext) {
        ConfigurableEnvironment env = appContext.getEnvironment();
        PropertyResolverProvider.environment = env;
        loadEnvironment(env, appContext);
    }

    private void loadEnvironment(ConfigurableEnvironment env, ApplicationContext appContext) {
        MutablePropertySources propertySources = env.getPropertySources();
        loadProperties(propertySources, env, appContext);
    }

    private void loadProperties(MutablePropertySources propertySources,
            Environment env,
            ApplicationContext appContext) {
        PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        String[] profiles = env == null ? null : env.getActiveProfiles();
        String appName = PropertySourcesLoader.getAppName(appContext);

        ConfigLog.LOG.info("Loading default properties...");
        propertySources.addLast(new DefaultPropertySource());

        ConfigLog.LOG.info("Loading preference properties...");
        propertySources.addBefore(DefaultPropertySource.PROPS_NAME, new PreferencePropertySource(appName));

        ConfigLog.LOG.info("Loading application properties...");
        propertySources.addBefore(PreferencePropertySource.PROPS_NAME,
            new ApplicationPropertySources(propertyResolver, appName, profiles));
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        this.appContext = appContext;
    }

    public static String getAppName(ApplicationContext appContext) {
        return normalizeAppName(appContext.getApplicationName());
    }

    public static String normalizeAppName(String appName) {
        if (appName.isEmpty()) {
            return "";
        }
        return appName.replace('/', ' ').replace('\\', ' ').trim().replace(' ', '-');
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (propertySources == null) {
            ConfigLog.LOG.info("The initialization of properties from the Spring configuration.");
            Environment env = appContext.getEnvironment();
            if (env instanceof ConfigurableEnvironment) {
                propertySources = ((ConfigurableEnvironment) env).getPropertySources();
            } else {
                propertySources = new MutablePropertySources();
                propertySources.addLast(new PropertySource<Environment>(ENVIRONMENT_PROPS, env) {
                    @Override
                    public String getProperty(String key) {
                        return this.source.getProperty(key);
                    }
                });
            }
            load(propertySources, env);
        }
        processProperties(beanFactory, propertySources);
    }

    private void load(MutablePropertySources propertySources, Environment env) {
        if (propertySources.contains(OPENL_ADDITIONAL_PROPS)) {
            ConfigLog.LOG
                .info("The second initialization of properties. The previous application properties have been kept.");
            return;
        } else if (!propertySources.contains(DefaultPropertySource.PROPS_NAME)) {
            ConfigLog.LOG.info("The first initialization of properties. Creating new application properties...");
            loadProperties(propertySources, env, appContext);
        }

        Properties properties;
        try {
            properties = mergeProperties();
        } catch (IOException ex) {
            ConfigLog.LOG.warn("Could not load properties", ex);
            return;
        }
        if (CollectionUtils.isEmpty(properties)) {
            ConfigLog.LOG.debug("Additional properties are absent.");
            return;
        }
        PropertiesPropertySource additionalProps = new PropertiesPropertySource(OPENL_ADDITIONAL_PROPS, properties);

        if (localOverride) {
            ConfigLog.LOG.info("Loading additional properties... Overriding the application properties...");
            propertySources.addBefore(ApplicationPropertySources.PROPS_NAME, additionalProps);
        } else {
            ConfigLog.LOG.info("Loading additional properties... Appending to the application properties...");
            propertySources.addAfter(ApplicationPropertySources.PROPS_NAME, additionalProps);

        }
    }

    @Override
    @Deprecated
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) {
        throw new UnsupportedOperationException(
            "Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");
    }

    protected void processProperties(ConfigurableListableBeanFactory beanFactory,
            PropertySources propertySources) throws BeansException {
        ConfigLog.LOG.info("Apply properties.");
        final PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
        propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
        propertyResolver.setValueSeparator(this.valueSeparator);

        StringValueResolver valueResolver = strVal -> {
            String resolved = ignoreUnresolvablePlaceholders ? propertyResolver.resolvePlaceholders(strVal)
                                                             : propertyResolver.resolveRequiredPlaceholders(strVal);
            return resolved.equals(nullValue) ? null : resolved;
        };

        doProcessProperties(beanFactory, valueResolver);
    }
}
