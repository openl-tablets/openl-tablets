package org.openl.rules.ruleservice.storelogdata;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

public final class PropertiesLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesLoader.class);

    private PropertiesLoader() {
    }

    private static boolean validateProperty(Environment env, String propName) {
        try {
            env.getProperty(propName);
            return true;
        } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
                if (e instanceof IllegalArgumentException) {
                    LOG.warn("Failed to load spring property '{}'. {}", propName, e.getMessage());
                } else {
                    LOG.warn("Failed to load spring property '{}'.", propName, e);
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
