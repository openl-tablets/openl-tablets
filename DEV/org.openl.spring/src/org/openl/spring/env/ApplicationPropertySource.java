package org.openl.spring.env;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.openl.util.CollectionUtils;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

/**
 * Application properties can be get as combinations of locations and names. If a location is a folder then it is
 * concatenated with each name. For example:
 * <p>
 * application name (from the Spring application context): OpenL Studio<br>
 * Spring active profiles: prod, qa<br>
 * locations: file:, file:openl.properties, file:openl/<br>
 * names: application.properties, {appName}.properties, {profile}.properties<br>
 * <br>
 * Then the list of resources to search is (next resource overides previous):
 * <ol>
 * <li>file:application.properties</li>
 * <li>file:WebStudio.properties</li>
 * <li>file:prod.properties</li>
 * <li>file:qa.properties</li>
 * <li>file:openl.properties</li>
 * <li>file:openl/application.properties</li>
 * <li>file:openl/WebStudio.properties</li>
 * <li>file:openl/prod.properties</li>
 * <li>file:openl/qa.properties</li>
 * </ol>
 * <p>
 * Default Application externalized configuration: <br>
 * Can be overridden using {@code openl.config.name} or {@code spring.config.name} properties for names. <br>
 * And {@code openl.config.location} or {@code spring.config.location} properties for folders and locations.
 * <ol>
 * <li>classpath:application*-default.properties</li>
 * <li>classpath:application.properties</li>
 * <li>classpath:{appName}.properties</li>
 * <li>classpath:config/application.properties</li>
 * <li>classpath:config/{appName}.properties</li>
 * <li>file:application.properties</li>
 * <li>file:{appName}.properties</li>
 * <li>file:conf/application.properties</li>
 * <li>file:conf/{appName}.properties</li>
 * <li>file:config/application.properties</li>
 * <li>file:config/{appName}.properties</li>
 * <li>file:${user.home}/application.properties</li>
 * <li>file:${user.home}/{appName}.properties</li>
 * <li>classpath:application-{profile}.properties</li>
 * <li>classpath:{appName}-{profile}.properties</li>
 * <li>classpath:config/application-{profile}.properties</li>
 * <li>classpath:config/{appName}-{profile}.properties</li>
 * <li>file:application-{profile}.properties</li>
 * <li>file:{appName}-{profile}.properties</li>
 * <li>file:conf/application-{profile}.properties</li>
 * <li>file:config/application-{profile}.properties</li>
 * <li>file:conf/{appName}-{profile}.properties</li>
 * <li>file:config/{appName}-{profile}.properties</li>
 * <li>file:${user.home}/application-{profile}.properties</li>
 * <li>file:${user.home}/{appName}-{profile}.properties</li>
 * </ol>
 *
 * @author Yury Molchan
 * @see <a href=
 * "https://docs.spring.io/spring-boot/docs/current/reference
 * /html/spring-boot-features.html#boot-features-external-config">Spring
 * Boot. Externalized Configuration.</a>
 */
public class ApplicationPropertySource extends EnumerablePropertySource<Deque<PropertySource<?>>> {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPropertySource.class);

    public static final String PROPS_NAME = "OpenL application properties";
    private static final String APP_NAME_TAG = "{appName}";
    private static final String PROFILE_TAG = "{profile}";
    private final String appName;
    private final String[] profiles;
    private final PropertyResolver resolver;
    private final Map<String, String> source = new HashMap<>();
    private final Map<String, String> profiledSource = new HashMap<>();

    ApplicationPropertySource(PropertyResolver resolver, String appName, String... profiles) {
        super(PROPS_NAME, new LinkedList<>());
        this.resolver = resolver;
        this.appName = appName;
        this.profiles = profiles;
        addLocations();
    }

    private static Object getInfo(Resource resource) {
        try {
            return resource.getURL();
        } catch (Exception e) {
            LOG.debug("Ignored error: ", e);
            return resource;
        }
    }

    private void addLocations() {
        String[] lc = resolvePlaceholders("${openl.config.location}");
        String[] nm = resolvePlaceholders("${openl.config.name}");
        for (String location : lc) {
            if (location.endsWith("/") || location.endsWith("\\") || location.endsWith(":")) {
                // Folder, schema root, Windows disk.
                for (String name : nm) {
                    addLocation(location + name);
                }
            } else {
                // direct location
                addLocation(location);
            }
        }
    }

    private String[] resolvePlaceholders(String value) {
        String resolved = resolver.resolvePlaceholders(value);
        if (StringUtils.isBlank(resolved)) {
            ConfigLog.LOG.debug("!       Empty: '{}'", value);
            return StringUtils.EMPTY_STRING_ARRAY;
        }
        return StringUtils.split(resolved, ',');
    }

    /**
     * The next source should override the previous.
     */
    private void addLocation(String location) {
        if (location.contains(APP_NAME_TAG)) {
            if (StringUtils.isBlank(appName)) {
                ConfigLog.LOG.debug("- No app name: '{}'", location);
            } else {
                location = location.replace(APP_NAME_TAG, appName);
            }
        }
        if (location.contains(PROFILE_TAG)) {
            if (CollectionUtils.isEmpty(profiles)) {
                ConfigLog.LOG.debug("- No profiles: '{}'", location);
            } else {
                for (String profile : profiles) {
                    addLocation(location.replace(PROFILE_TAG, profile), true);
                }
            }
        } else {
            addLocation(location, false);
        }
    }

    private void addLocation(String location, boolean isProfiled) {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(location);
        } catch (IOException e) {
            ConfigLog.LOG.debug("!     Error: '{}'", location, e);
            return;
        }
        if (CollectionUtils.isEmpty(resources)) {
            ConfigLog.LOG.debug("- Not found: [{}]", location);
            return;
        }

        Arrays.sort(resources,
                Comparator.comparing(Resource::getFilename,
                        Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())));
        for (Resource resource : resources) {
            try {
                if (resource.exists()) {
                    Map<String, String> props = new HashMap<>();
                    try (var in = resource.getInputStream()) {
                        PropertiesUtils.load(in, props::put);
                    }
                    if (isProfiled) {
                        profiledSource.putAll(props);
                    } else {
                        source.putAll(props);
                    }
                    ConfigLog.LOG.info("+       Load: [{}] '{}' ({} properties)", location, getInfo(resource), props.size());

                } else {
                    ConfigLog.LOG.debug("- Not exist: [{}] '{}'", location, getInfo(resource));
                }
            } catch (Exception ex) {
                ConfigLog.LOG.debug("!     Error: [{}] '{}'", location, getInfo(resource), ex);
            }
        }
    }

    @Override
    public Object getProperty(String name) {

        Object propertyInternal = getPropertyInternal(name);
        if (propertyInternal != null) {
            String value = propertyInternal.toString();
            value = StringUtils.trimToEmpty(value);
            return DynamicPropertySource.decode(value);
        }
        return propertyInternal;
    }

    private Object getPropertyInternal(String name) {
        Object candidate = profiledSource.get(name);
        if (candidate != null) {
            return candidate;
        }
        return source.get(name);
    }

    @Override
    public boolean containsProperty(String name) {
        return profiledSource.containsKey(name) || source.containsKey(name);
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> names = new TreeSet<>();
        names.addAll(profiledSource.keySet());
        names.addAll(source.keySet());
        return names.toArray(StringUtils.EMPTY_STRING_ARRAY);
    }
}
