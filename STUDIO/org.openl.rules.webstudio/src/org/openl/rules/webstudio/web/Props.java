package org.openl.rules.webstudio.web;

import org.openl.spring.env.DisablePropertySource;
import org.openl.util.StringUtils;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class Props {
    private static Environment env;

    public static String text(String key) {
        return env.getProperty(key);
    }

    public static boolean bool(String key) {
        return Boolean.parseBoolean(text(key));
    }

    public static Integer integer(String key) {
        String text = text(key);
        return StringUtils.isNotEmpty(text) ? Integer.valueOf(text) : null;
    }

    public static void setEnvironment(Environment environment) {
        env = environment;
    }

    public static boolean isDisabled(String name) {
        return Props.bool(DisablePropertySource.PROPS_PREFIX + name);
    }

}
