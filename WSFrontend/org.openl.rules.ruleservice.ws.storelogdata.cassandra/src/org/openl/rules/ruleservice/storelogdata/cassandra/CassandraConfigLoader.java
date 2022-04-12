package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.util.Properties;

import org.openl.rules.ruleservice.storelogdata.PropertiesLoader;
import org.openl.spring.config.ConditionalOnEnable;
import org.springframework.context.ApplicationContext;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.Parseable;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnEnable("ruleservice.store.logs.cassandra.enabled")
public class CassandraConfigLoader {

    public final ApplicationContext context;
    private volatile Properties appContextProperties;

    public CassandraConfigLoader(ApplicationContext context) {
        this.context = context;
    }

    private Properties getAppContextProperties() {
        if (this.appContextProperties == null) {
            synchronized (this) {
                if (this.appContextProperties == null) {
                    this.appContextProperties = PropertiesLoader.getApplicationContextProperties(context);
                }
            }
        }
        return this.appContextProperties;
    }

    public DriverConfigLoader getDriverConfigLoader() {
        return new DefaultDriverConfigLoader(() -> {
            ConfigFactory.invalidateCaches();
            Config config = ConfigFactory.defaultOverrides()
                .withFallback(
                    Parseable
                        .newProperties(getAppContextProperties(),
                            ConfigParseOptions.defaults().setOriginDescription("spring context properties"))
                        .parse())
                .withFallback(ConfigFactory.defaultReference())
                .resolve();
            return config.getConfig(DefaultDriverConfigLoader.DEFAULT_ROOT_PATH);
        });
    }

}
