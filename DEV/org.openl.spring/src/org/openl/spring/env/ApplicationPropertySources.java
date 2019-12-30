package org.openl.spring.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

/**
 * A helper which resolves ${...} placeholders with values from property resources (system & environment properties,
 * init-params, predefined properties files...), then converts a comma delimited string to a list of values and replaces
 * and {appName} and {profile} tags with values from the application context.
 * <p>
 * For example. If value is <i>file:${user.home}/{appName}-{profile}.properties</i>, where {appName} is "webstudio", and
 * Spring active propfile is "prod, test", then result will be the list of values:
 * <i>["file:/home/openl/webstudio-prod.properties", "file:/home/openl/webstudio-test.properties"]</i>
 * </p>
 *
 * @author Yury Molchan
 */
public class ApplicationPropertySources extends CompositePropertySource {
    public static final String PROPS_NAME = "OpenL application properties";
    private static final String APP_NAME_TAG = "{appName}";
    private static final String PROFILE_TAG = "{profile}";
    private final String appName;
    private final String[] profiles;
    private final PropertyResolver resolver;

    ApplicationPropertySources(PropertyResolver resolver, String appName, String... profiles) {
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
