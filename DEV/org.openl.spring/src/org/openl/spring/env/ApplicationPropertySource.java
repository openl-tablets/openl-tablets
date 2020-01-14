package org.openl.spring.env;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Application properties can be get as combinations of locations and names. If a location is a folder then it is
 * concatenated with each name. For example:
 * <p>
 * application name (from the Spring application context): WebStudio<br>
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
 * 
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
 * @see <a href=
 *      "https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config">Spring
 *      Boot. Externalized Configuration.</a>
 * @author Yury Molchan
 */
public class ApplicationPropertySource extends EnumerablePropertySource<Deque<PropertySource<?>>> {
    public static final String PROPS_NAME = "OpenL application properties";
    private static final String APP_NAME_TAG = "{appName}";
    private static final String PROFILE_TAG = "{profile}";
    private final String appName;
    private final String[] profiles;
    private final PropertyResolver resolver;
    private final LinkedList<EnumerablePropertySource> source = new LinkedList<>();
    private final LinkedList<EnumerablePropertySource<?>> profiledSource = new LinkedList<>();

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

    private List<String> resolveProfile(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        if (value.contains(PROFILE_TAG)) {
            if (CollectionUtils.isEmpty(profiles)) {
                ConfigLog.LOG.debug("- No profiles: '{}'", value);
                return Collections.emptyList();
            } else {
                ArrayList<String> result = new ArrayList<>(profiles.length);
                for (String profile : profiles) {
                    result.add(value.replace(PROFILE_TAG, profile));
                }
                return result;
            }
        } else {
            return Collections.singletonList(value);
        }
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
                addLocation(location, true);
            }
        } else {
            addLocation(location, false);
        }
    }

    private void addLocation(String location, boolean appNameResolved) {
        if (location.contains(PROFILE_TAG)) {
            if (CollectionUtils.isEmpty(profiles)) {
                ConfigLog.LOG.debug("- No profiles: '{}'", location);
            } else {
                for (String profile : profiles) {
                    addLocation(location.replace(PROFILE_TAG, profile), appNameResolved, true);
                }
            }
        } else {
            addLocation(location, appNameResolved, false);
        }
    }

    private void addLocation(String location, boolean appNameResolved, boolean isProfiled) {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(location);
        } catch (IOException e) {
            ConfigLog.LOG.debug("!     Error: '{}'", new Object[] { location, e });
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
                    EnumerablePropertySource<?> propertySource;
                    if (appNameResolved || StringUtils.isBlank(appName)) {
                        propertySource = new ResourcePropertySource(resource);
                    } else {
                        propertySource = new ResourcePropertySource(resource) {
                            @Override
                            public Object getProperty(String name) {
                                // get the first appName.property.name, and then property.name
                                Object property = super.getProperty(appName + "." + name);
                                return property != null ? property : super.getProperty(name);
                            }
                        };
                    }
                    if (isProfiled) {
                        profiledSource.addFirst(propertySource);
                    } else {
                        source.addFirst(propertySource);
                    }
                    ConfigLog.LOG.info("+        Add: [{}] '{}'", location, getInfo(resource));
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
        for (PropertySource<?> propertySource : profiledSource) {
            Object candidate = propertySource.getProperty(name);
            if (candidate != null) {
                return candidate;
            }
        }
        for (PropertySource<?> propertySource : source) {
            Object candidate = propertySource.getProperty(name);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean containsProperty(String name) {
        for (PropertySource<?> propertySource : profiledSource) {
            if (propertySource.containsProperty(name)) {
                return true;
            }
        }
        for (PropertySource<?> propertySource : source) {
            if (propertySource.containsProperty(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> names = new LinkedHashSet<>();
        for (EnumerablePropertySource<?> propertySource : profiledSource) {
            names.addAll(Arrays.asList(propertySource.getPropertyNames()));
        }
        for (EnumerablePropertySource<?> propertySource : source) {
            names.addAll(Arrays.asList(propertySource.getPropertyNames()));
        }
        return names.toArray(new String[0]);
    }
}
