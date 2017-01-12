package org.openl.spring.env;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.StringValueResolver;

/**
 * Replacement of
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer}.
 * Allows to combine environment properties, default application properties and
 * external application properties. Application properties can be get as
 * combinations of locations and names. If a location is a folder then it is
 * concatenated with each name. For example:
 * <p>
 * application name (from the Spring application context): WebStudio<br>
 * Spring active profiles: prod, qa<br>
 * locations: file:, file:openl.properties, file:openl/<br>
 * names: application.properties, {appName}.properties, {profile}.properties<br>
 * defaults: classpath*:openl-default.properties <br>
 * <br>
 * Then the list of resources to search is (next resource overides previous):
 * <ol>
 * <li>file:foo.jar!/openl-default.properties</li>
 * <li>file:bar.jar!/openl-default.properties</li>
 * <li>classpath:openl-default.properties</li>
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
 * <li>In the web.xml, using
 * {@linkplain org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * "contextInitializerClasses"} context parameter.</li>
 * <li>In Spring configuration, declaring
 * {@code <bean class="org.openl.spring.env.PropertySourcesLoader"/>} bean.</li>
 * </ul>
 * Default resolving order (next resource overides previous):
 * <ul>
 * <p>
 * <li>OpenL default properties. <br>
 * Can be overridden using {@code openl.config.default} property.
 * <ol>
 * <li>classpath*:openl-default.properties</li>
 * <li>classpath:/org/openl/main/openl.version.properties</li>
 * </ol>
 * </li>
 * <li>Application externalized configuration. <br>
 * Can be overridden using {@code openl.config.name} or
 * {@code spring.config.name} properties for names. <br>
 * And {@code openl.config.location} or {@code spring.config.location}
 * properties for folders and locations.
 * <ol>
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
 * <li>Servlet context init parameters from
 * {@link javax.servlet.ServletContext#getInitParameter(java.lang.String)}</li>
 * <li>Servlet config init parameters from
 * {@link javax.servlet.ServletConfig#getInitParameter(java.lang.String)}</li>
 * </ol>
 * </li>
 * <p>
 * </ol>
 * To override dafault can be used the following properies:
 * <ul>
 * <li>{@code openl.config.default} - Default configs</li>
 * <li></li>
 * <li></li>
 * </ul>
 *
 * @author Yury Molchan
 * @see org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * @see org.springframework.beans.factory.config.PlaceholderConfigurerSupport
 * @see org.springframework.context.ApplicationContextInitializer
 * @see PropertyResourceResolver
 */
public class PropertySourcesLoader extends PlaceholderConfigurerSupport implements ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationContextAware {
    public static final String OPENL_INIT_PROPS = "openlInitProps";
    public static final String OPENL_INIT_DEFAULT_PROPS = "openlInitDefaultProps";
    public static final String OPENL_INIT_APPLICATION_PROPS = "openlInitApplicationProps";
    public static final String OPENL_DEFAULT_PROPS = "openlDefaultProps";
    public static final String OPENL_APPLICATION_PROPS = "openlApplicationProps";
    public static final String ENVIRONMENT_PROPS = "environmentProps";
    private static final String DEFAULT_LOCATIONS = "${openl.config.location}";
    private static final String DEFAULT_NAMES = "${openl.config.name}";
    private static final String DEFAULT_DEFAULTS = "${openl.config.default}";
    private String[] locations;
    private String[] names;
    private String[] defaults;
    private ApplicationContext appContext;
    private PropertyResourceResolver resolver;
    private MutablePropertySources propertySources;

    /**
     * @param locations
     */
    public void setLocations(String... locations) {
        this.locations = locations;
    }

    public void setNames(String... names) {
        this.names = names;
    }

    public void setDefaults(String... defaults) {
        this.defaults = defaults;
    }

    @Override
    public void initialize(ConfigurableApplicationContext appContext) {
        log.info("!   The initialization of properties from web.xml");
        setApplicationContext(appContext);
        ConfigurableEnvironment env = appContext.getEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        load(propertySources, env);
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        this.appContext = appContext;
    }

    private void load(MutablePropertySources propertySources, Environment env) {
        try {
            PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
            String[] profiles = env == null ? null : env.getActiveProfiles();
            resolver = new PropertyResourceResolver(propertyResolver, getAppName(appContext), profiles);
            resolver.debug = debug;
            boolean alreadyInit = propertySources.contains(OPENL_INIT_PROPS);
            CompositePropertySource defaultProps;
            CompositePropertySource applicationProps;
            if (!alreadyInit) {
                log.info("!   The first initialization of properties.");
                Resource initConfig = appContext.getResource("classpath:openl-default-config.properties");
                propertySources.addLast(new ResourcePropertySource(OPENL_INIT_PROPS, initConfig));
                defaultProps = new CompositePropertySource(OPENL_INIT_DEFAULT_PROPS);
                propertySources.addAfter(OPENL_INIT_PROPS, defaultProps);
                applicationProps = new CompositePropertySource(OPENL_INIT_APPLICATION_PROPS);
                propertySources.addAfter(OPENL_INIT_PROPS, applicationProps);
            } else {
                log.info("!   The seconds initialization of properties. Does it override the first initialization ? - {}.", localOverride);
                defaultProps = new CompositePropertySource(OPENL_DEFAULT_PROPS);
                propertySources.addBefore(OPENL_INIT_DEFAULT_PROPS, defaultProps);
                applicationProps = new CompositePropertySource(OPENL_APPLICATION_PROPS);
                if (localOverride) {
                    propertySources.addBefore(OPENL_INIT_APPLICATION_PROPS, applicationProps);
                } else {
                    propertySources.addAfter(OPENL_INIT_APPLICATION_PROPS, applicationProps);
                }
            }

            addDefaultProps(defaultProps, alreadyInit);
            addApplicationProps(applicationProps, alreadyInit);
            log.info("openl.home = {}", propertyResolver.getProperty("openl.home"));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load properties", ex);
        }
    }

    private void addDefaultProps(CompositePropertySource propertySource, boolean alreadyInit) throws IOException {
        List<String> locations = resolvePlaceholders(defaults, DEFAULT_DEFAULTS, alreadyInit);
        for (String location : locations) {
            addResource(propertySource, location);
        }
    }

    private void addApplicationProps(CompositePropertySource propertySource, boolean alreadyInit) {
        List<String> lc = resolvePlaceholders(locations, DEFAULT_LOCATIONS, alreadyInit);
        List<String> nm = resolvePlaceholders(names, DEFAULT_NAMES, alreadyInit);
        for (String location : lc) {
            if (location.endsWith("/") || location.endsWith("\\") || location.endsWith(":")) {
                // Folder, schema root, Windows disk.
                for (String name : nm) {
                    addResource(propertySource, location + name);
                }
            } else {
                // direct location
                addResource(propertySource, location);
            }
        }
    }

    private List<String> resolvePlaceholders(String[] values, String def, boolean alreadyInit) {
        if (values != null) {
            return resolver.resolvePlaceholders(values);
        } else if (alreadyInit) {
            // Defaults already registered.
            return Collections.emptyList();
        } else {
            return resolver.resolvePlaceholders(def);
        }
    }

    private String getAppName(ApplicationContext appContext) {
        String appName = appContext.getApplicationName();
        if (appName == null || appName.isEmpty()) {
            return "";
        }
        return appName.replace('/', ' ').replace('\\', ' ').trim().replace(' ', '-');
    }

    private void addResource(CompositePropertySource propertySource, String location) {
        List<String> list = resolver.resolvePlaceholders(location);
        for (String value : list) {
            PropertySource resources = loadResource(value);
            // To preserve overriding order we iterate from the end.
            // The next resource should override the previous.
            propertySource.addFirst(resources);
        }
    }

    private PropertySource loadResource(String location) {
        if (location == null) {
            return null;
        }
        if (location.matches("[\\{\\}]")) {
            log.info("!  Unresolved: '{}'", location);
        }
        Resource[] resources;
        try {
            resources = appContext.getResources(location);
        } catch (IOException e) {
            debug("!       Error: '{}'", location, e);
            return null;
        }
        if (CollectionUtils.isEmpty(resources)) {
            debug("-   Not found: '{}'", location);
            return null;
        }
        CompositePropertySource propertySource = new CompositePropertySource(location);
        for (Resource resource : resources) {
            try {
                if (resource.exists()) {
                    propertySource.addFirst(new ResourcePropertySource(resource));
                    log.info("+         Add: [{}] '{}'", location, getInfo(resource));
                } else {
                    debug("-   Not exist: [{}] '{}'", location, resource);
                }
            } catch (Exception ex) {
                debug("!       Error: [{}] '{}'", location, resource, ex);
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
            log.info("!   The initialization of properties from the Spring configuration.");
            Environment env = appContext.getEnvironment();
            if (env instanceof ConfigurableEnvironment) {
                propertySources = ((ConfigurableEnvironment) env).getPropertySources();
            } else {
                propertySources = new MutablePropertySources();
                if (env != null) {
                    this.propertySources.addLast(new PropertySource<Environment>(ENVIRONMENT_PROPS, env) {
                        @Override
                        public String getProperty(String key) {
                            return this.source.getProperty(key);
                        }
                    });
                }
            }
            load(propertySources, env);
        }
        processProperties(beanFactory, propertySources);
    }

    @Override
    @Deprecated
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) {
        throw new UnsupportedOperationException(
            "Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");
    }

    protected void processProperties(ConfigurableListableBeanFactory beanFactory,
            PropertySources propertySources) throws BeansException {
        log.info("!   Apply properties.");
        final PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
        propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
        propertyResolver.setValueSeparator(this.valueSeparator);

        StringValueResolver valueResolver = new StringValueResolver() {
            @Override
            public String resolveStringValue(String strVal) {
                String resolved = ignoreUnresolvablePlaceholders ? propertyResolver.resolvePlaceholders(strVal)
                                                                 : propertyResolver.resolveRequiredPlaceholders(strVal);
                return (resolved.equals(nullValue) ? null : resolved);
            }
        };

        doProcessProperties(beanFactory, valueResolver);
    }

    private final Logger log = LoggerFactory.getLogger(PropertySourcesLoader.class);
    private boolean debug;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private void debug(String message, Object... resource) {
        if (debug) {
            log.info(message, resource);
        } else {
            log.debug(message, resource);
        }
    }
}
