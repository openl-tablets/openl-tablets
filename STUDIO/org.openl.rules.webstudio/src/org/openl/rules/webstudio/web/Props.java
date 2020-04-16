package org.openl.rules.webstudio.web;

import org.openl.spring.env.DisablePropertySource;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class Props implements EnvironmentAware {
    private static Environment env;

    public static String text(String key) {
        return env.getProperty(key);
    }

    public static boolean bool(String key) {
        return Boolean.parseBoolean(text(key));
    }

    @Override
    public void setEnvironment(Environment environment) {
        env = environment;
    }

    public boolean isPropertyDisabled(String name) {
        return Props.bool(DisablePropertySource.PROPS_PREFIX + name);
    }

}
