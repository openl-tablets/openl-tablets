package org.openl.rules.ruleservice.storelogdata;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.StreamSupport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

@Slf4j
public final class PropertiesLoader {

    private PropertiesLoader() {
    }

    private static boolean validateProperty(Environment env, String propName) {
        try {
            return env.getProperty(propName) != null;
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                if (e instanceof IllegalArgumentException) {
                    log.warn("Failed to load spring property '{}'. {}", propName, e.getMessage(), e);
                } else {
                    log.warn("Failed to load spring property '{}'.", propName, e);
                }
            }
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    public static Properties getApplicationContextProperties(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        Properties props = new Properties();
        MutablePropertySources propSources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(propSources.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .filter(propName -> validateProperty(env, propName))
                .forEach(propName -> props.setProperty(propName, env.getProperty(propName)));
        return props;
    }
}
