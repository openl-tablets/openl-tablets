package org.openl.rules.ruleservice.storelogdata.cassandra;

import org.openl.rules.ruleservice.storelogdata.PropertiesLoader;
import org.springframework.context.ApplicationContext;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.Parseable;

final class ConfigLoader {

    private ConfigLoader() {
    }

    private static ConfigObject loadApplicationContextProperties(ApplicationContext applicationContext) {
        return Parseable
            .newProperties(PropertiesLoader.getApplicationContextProperties(applicationContext),
                ConfigParseOptions.defaults().setOriginDescription("spring context properties"))
            .parse();
    }

    static DriverConfigLoader fromApplicationContext(ApplicationContext applicationContext) {
        return new DefaultDriverConfigLoader(() -> {
            ConfigFactory.invalidateCaches();
            Config config = ConfigFactory.defaultOverrides()
                .withFallback(loadApplicationContextProperties(applicationContext))
                .withFallback(ConfigFactory.defaultReference())
                .resolve();
            return config.getConfig(DefaultDriverConfigLoader.DEFAULT_ROOT_PATH);
        });
    }

}
