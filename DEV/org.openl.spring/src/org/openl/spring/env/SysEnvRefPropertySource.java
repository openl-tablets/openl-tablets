package org.openl.spring.env;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * Customizes {@link SystemEnvironmentPropertySource} by removing `$` sing from begging of property token and wraps it with `_`.
 * <p>
 * Example:
 * foo.$bar -> foo._bar_
 * foo.$bar.gaz -> foo._bar_.gaz
 * <p>
 * Must be registered instead of default {@link SystemEnvironmentPropertySource}
 *
 * @see org.springframework.core.env.StandardEnvironment
 */
public class SysEnvRefPropertySource extends SystemEnvironmentPropertySource {

    // Lookup property tokens started with $ sign in the keys like:
    // foo.$bar
    // foo.$bar.bar
    private static final Pattern DOLLAR_LITERAL = Pattern.compile("(?<=[._])\\$([^._]*)(?=[._])?");

    public SysEnvRefPropertySource(Map<String, Object> source) {
        super(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, source);
        ConfigLog.LOG.info("Loading System Environment parameters: {} properties.", getPropertyNames().length);
    }

    @Override
    public Object getProperty(String name) {
        Object result = super.getProperty(name);
        if (result != null) {
            return result;
        }
        Matcher matcher = DOLLAR_LITERAL.matcher(name);
        if (matcher.find()) {
            // replace $ref with _ref_
            name = matcher.replaceAll("_$1_");
            return super.getProperty(name);
        }
        return null;
    }

}
