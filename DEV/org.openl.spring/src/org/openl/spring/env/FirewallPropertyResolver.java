package org.openl.spring.env;

import java.util.regex.Pattern;

import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import org.openl.util.StringUtils;

/**
 * This resolver allows to control which property keys can be resolved.
 * Provides ability to decline reading keys matched to the pattern.
 * <p>
 * Also to get a property value without interpolating of placeholder as is, for internal usage only.
 *
 * @author Yury Molchan
 */
class FirewallPropertyResolver extends PropertySourcesPropertyResolver {

    private Pattern allowedPattern;
    private Pattern deniedPattern;

    FirewallPropertyResolver(PropertySources propertySources) {
        super(propertySources);
    }

    void initFirewall() {
        String allowedRegex = getProperty("openl.config.key-pattern.allowed");
        String deniedRegex = getProperty("openl.config.key-pattern.denied");
        if (StringUtils.isNotBlank(allowedRegex)) {
            allowedPattern = Pattern.compile(allowedRegex);
        }
        if (StringUtils.isNotBlank(deniedRegex)) {
            deniedPattern = Pattern.compile(deniedRegex);
        }
    }

    /**
     * Checks if the property key meets to requirements for reading. Returns {@code null}, if the property is denied
     */
    @Override
    protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
        if (deniedPattern != null && deniedPattern.matcher(key).matches()) {
            ConfigLog.LOG.warn("     Denied: '{}'", key);
            return null;
        }
        if (allowedPattern != null && !allowedPattern.matcher(key).matches()) {
            ConfigLog.LOG.warn("Not allowed: '{}'", key);
            return null;
        }
        return super.getProperty(key, targetValueType, resolveNestedPlaceholders);
    }

    String getRawProperty(String key) {
        return getPropertyAsRawString(key);
    }

}
