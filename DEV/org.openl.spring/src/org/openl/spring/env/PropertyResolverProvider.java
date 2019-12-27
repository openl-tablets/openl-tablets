package org.openl.spring.env;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

public class PropertyResolverProvider implements EnvironmentAware {

    private static PropertyResolver environment;

    @Override
    public void setEnvironment(Environment env) {
        environment = env;
    }

    public static String getProperty(String propertyName) {
        if (environment != null) {
            return environment.getProperty(propertyName);
        }
        return null;
    }

    public static PropertyResolver getEnvironment() {
        return environment;
    }
}
