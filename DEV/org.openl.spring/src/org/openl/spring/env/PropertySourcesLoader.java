package org.openl.spring.env;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.openl.info.OpenLVersion;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertySource;
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
 * @see PropertyResourceResolver
 */
public class PropertySourcesLoader extends PlaceholderConfigurerSupport implements ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationContextAware {
    public static final String OPENL_DEFAULT_PROPS = "OpenL default properties";
    public static final String OPENL_PREFERENCE_PROPS = "OpenL preference properties";
    public static final String OPENL_APPLICATION_PROPS = "OpenL application properties";
    public static final String OPENL_ADDITIONAL_PROPS = "OpenL additional properties";
    public static final String ENVIRONMENT_PROPS = "environmentProps";
    private static final String VERSION = OpenLVersion.getVersion(); // Just for init OpenLVersion class.
    private final Logger log = LoggerFactory.getLogger("OpenL.config");
    private ApplicationContext appContext;
    private MutablePropertySources propertySources;

    {
        setIgnoreResourceNotFound(true);
    }

    @Override
    public void initialize(ConfigurableApplicationContext appContext) {
        log.info("The initialization of properties from 'contextInitializerClasses' context-param in web.xml");
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
        setApplicationContext(appContext);
        ConfigurableEnvironment env = appContext.getEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();

        loadProperties(propertySources, env);
    }

    private void loadProperties(MutablePropertySources propertySources, Environment env) {
        PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        String[] profiles = env == null ? null : env.getActiveProfiles();
        String appName = getAppName(appContext);
        PropertyResourceResolver resolver = new PropertyResourceResolver(propertyResolver, appName, profiles);

        log.info("Loading default properties...");
        CompositePropertySource defaultProps = new CompositePropertySource(OPENL_DEFAULT_PROPS);
        List<String> locations = resolver.resolvePlaceholders("classpath*:openl-default.properties",
            "${openl.config.default}");
        for (String location : locations) {
            addResource(defaultProps, location);
        }
        propertySources.addLast(defaultProps);

        log.info("Loading preference properties...");
        PreferencePropertySource preferenceProps = new PreferencePropertySource(OPENL_PREFERENCE_PROPS, appName);
        propertySources.addBefore(OPENL_DEFAULT_PROPS, preferenceProps);

        log.info("Loading application properties...");
        CompositePropertySource applicationProps = new CompositePropertySource(OPENL_APPLICATION_PROPS);
        List<String> lc = resolver.resolvePlaceholders("${openl.config.location}");
        List<String> nm = resolver.resolvePlaceholders("${openl.config.name}");
        for (String location : lc) {
            if (location.endsWith("/") || location.endsWith("\\") || location.endsWith(":")) {
                // Folder, schema root, Windows disk.
                for (String name : nm) {
                    addResource(applicationProps, location + name);
                }
            } else {
                // direct location
                addResource(applicationProps, location);
            }
        }
        propertySources.addBefore(OPENL_PREFERENCE_PROPS, applicationProps);
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

    private void addResource(CompositePropertySource propertySource, String location) {
        PropertySource<?> found = find(location, appContext);
        // To preserve overriding order we iterate from the end.
        // The next resource should override the previous.
        propertySource.addFirst(found);
    }

    /**
     * The next source should override the previous.
     */
    private PropertySource<?> find(String location, ResourcePatternResolver resourcePattern) {
        if (location == null) {
            return null;
        }
        if (location.matches("[\\{\\}]")) {
            log.info("! Unresolved: '{}'", location);
        }
        Resource[] resources;
        try {
            resources = resourcePattern.getResources(location);
        } catch (IOException e) {
            log.debug("!     Error: '{}'", new Object[] { location, e });
            return null;
        }
        if (CollectionUtils.isEmpty(resources)) {
            log.debug("- Not found: [{}]", location);
            return null;
        }
        Arrays.sort(resources,
            Comparator.comparing(Resource::getFilename,
                Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())));
        CompositePropertySource propertySource = new CompositePropertySource(location);
        for (Resource resource : resources) {
            try {
                if (resource.exists()) {
                    propertySource.addFirst(new ResourcePropertySource(resource));
                    log.info("+        Add: [{}] '{}'", location, getInfo(resource));
                } else {
                    log.debug("- Not exist: [{}] '{}'", new Object[] { location, getInfo(resource) });
                }
            } catch (Exception ex) {
                log.debug("!     Error: [{}] '{}'", location, getInfo(resource), ex);
            }
        }
        return propertySource.get();
    }

    private Object getInfo(Resource resource) {
        try {
            return resource.getURL();
        } catch (Exception e) {
            return resource;
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (propertySources == null) {
            log.info("The initialization of properties from the Spring configuration.");
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
            log.info("The second initialization of properties. The previous application properties have been kept.");
            return;
        } else if (!propertySources.contains(OPENL_DEFAULT_PROPS)) {
            log.info("The first initialization of properties. Creating new application properties...");
            loadProperties(propertySources, env);
        }

        Properties properties;
        try {
            properties = mergeProperties();
        } catch (IOException ex) {
            log.warn("Could not load properties", ex);
            return;
        }
        if (CollectionUtils.isEmpty(properties)) {
            log.debug("Additional properties are absent.");
            return;
        }
        PropertiesPropertySource additionalProps = new PropertiesPropertySource(OPENL_ADDITIONAL_PROPS, properties);

        if (localOverride) {
            log.info("Loading additional properties... Overriding the application properties...");
            propertySources.addBefore(OPENL_APPLICATION_PROPS, additionalProps);
        } else {
            log.info("Loading additional properties... Appending to the application properties...");
            propertySources.addAfter(OPENL_APPLICATION_PROPS, additionalProps);

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
        log.info("Apply properties.");
        final PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
        propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
        propertyResolver.setValueSeparator(this.valueSeparator);

        StringValueResolver valueResolver = new StringValueResolver() {
            @Override
            public String resolveStringValue(String strVal) {
                String resolved = ignoreUnresolvablePlaceholders ? propertyResolver.resolvePlaceholders(strVal)
                                                                 : propertyResolver.resolveRequiredPlaceholders(strVal);
                return resolved.equals(nullValue) ? null : resolved;
            }
        };

        doProcessProperties(beanFactory, valueResolver);
    }
}
