package org.openl.spring.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

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
 * <li>classpath:application-{profile}.properties</li>
 * <li>classpath:{appName}.properties</li>
 * <li>classpath:{appName}-{profile}.properties</li>
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
 * </ol>
 *
 * @author Yury Molchan
 */
public class ApplicationPropertySource extends CompositePropertySource {
    public static final String PROPS_NAME = "OpenL application properties";
    private static final String APP_NAME_TAG = "{appName}";
    private static final String PROFILE_TAG = "{profile}";
    private final String appName;
    private final String[] profiles;
    private final PropertyResolver resolver;

    ApplicationPropertySource(PropertyResolver resolver, String appName, String... profiles) {
        super(PROPS_NAME);
        this.resolver = resolver;
        this.appName = appName;
        this.profiles = profiles;
        addLocations();
    }

    private void addLocations() {
        List<String> lc = resolvePlaceholders("${openl.config.location}");
        List<String> nm = resolvePlaceholders("${openl.config.name}");
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

    private List<String> resolvePlaceholders(String value) {
        ArrayList<String> result = new ArrayList<>(32);
        String resolved = resolver.resolvePlaceholders(value);
        if (StringUtils.isBlank(resolved)) {
            ConfigLog.LOG.debug("!       Empty: '{}'", value);
            return Collections.emptyList();
        }
        String[] splitted = StringUtils.split(resolved, ',');
        for (String s : splitted) {
            String withApp = resolveAppName(s);
            List<String> withTags = resolveProfile(withApp);
            result.addAll(withTags);
        }
        return result;
    }

    private String resolveAppName(String value) {
        if (value.contains(APP_NAME_TAG)) {
            if (StringUtils.isBlank(appName)) {
                ConfigLog.LOG.debug("- No app name: '{}'", value);
                value = "";
            } else {
                value = value.replace(APP_NAME_TAG, appName);
            }
        }
        return value;
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
}
