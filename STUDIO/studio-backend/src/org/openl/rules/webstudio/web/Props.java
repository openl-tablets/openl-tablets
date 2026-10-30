package org.openl.rules.webstudio.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import org.openl.spring.env.DisablePropertySource;
import org.openl.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Props {

    private static Environment env;

    public static String text(String key) {
        return env.getProperty(key);
    }

    public static boolean bool(String key) {
        return Boolean.parseBoolean(text(key));
    }

    public static @Nullable Integer integer(String key) {
        String text = text(key);
        return StringUtils.isNotEmpty(text) ? Integer.valueOf(text) : null;
    }

    public static Environment getEnvironment() {
        return env;
    }

    public static void setEnvironment(Environment environment) {
        env = environment;
    }

    public static boolean isDisabled(String name) {
        return isDisabled(env, name);
    }

    /**
     * Whether an environment variable or a system property sets the property, directly or through a {@code $ref}
     * key, so the application settings cannot change it.
     *
     * <p>The answer is read past the key patterns of {@code openl.config.key-pattern}: it tells only whether the
     * property is set outside, never its value. Without an environment, nothing is set outside.
     */
    public static boolean isDisabled(@Nullable Environment environment, String name) {
        if (environment == null) {
            return false;
        }
        var key = DisablePropertySource.PROPS_PREFIX + name;
        var disable = environment instanceof ConfigurableEnvironment configurable
                ? configurable.getPropertySources().get(DisablePropertySource.PROPS_NAME)
                : null;
        var value = disable != null ? disable.getProperty(key) : environment.getProperty(key);
        return Boolean.parseBoolean(String.valueOf(value));
    }

}
