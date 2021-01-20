package org.openl.rules.ext.cassandra;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static DriverConfigLoader fromProjectResource(String resource) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ConfigLoader.class.getClassLoader());
            return new DefaultDriverConfigLoader(() -> {
                ConfigFactory.invalidateCaches();
                Config config = ConfigFactory.defaultOverrides()
                    .withFallback(ConfigFactory.parseResourcesAnySyntax(ConfigLoader.class.getClassLoader(), resource))
                    .withFallback(ConfigFactory.defaultReference())
                    .resolve();
                return config.getConfig(DefaultDriverConfigLoader.DEFAULT_ROOT_PATH);
            });
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
