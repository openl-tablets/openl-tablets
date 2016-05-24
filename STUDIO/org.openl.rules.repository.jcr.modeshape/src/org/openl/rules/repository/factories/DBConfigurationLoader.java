package org.openl.rules.repository.factories;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DBConfigurationLoader {
    private DBConfigurationLoader() {
    }

    private static CompositeConfiguration getConfiguration(String databaseName) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();

        // Configuration for specific DB. Can be absent
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setDelimiterParsingDisabled(true);
        configuration.setFileName("modeshape-" + databaseName + ".properties");
        try {
            configuration.load();
            compositeConfiguration.addConfiguration(configuration);
        } catch (ConfigurationException e) {
            Logger logger = LoggerFactory.getLogger(DBRepositoryFactory.class);
            logger.debug("Configuration: {} file is absent", "modeshape-" + databaseName + ".properties", e);
        }

        // Default configuration
        configuration = new PropertiesConfiguration();
        configuration.setDelimiterParsingDisabled(true);
        configuration.setFileName("modeshape.properties");
        try {
            configuration.load();
        } catch (ConfigurationException e) {
            Logger logger = LoggerFactory.getLogger(DBRepositoryFactory.class);
            logger.error("Error when initializing configuration: {}", "modeshape.properties", e);
        }
        compositeConfiguration.addConfiguration(configuration);

        return compositeConfiguration;
    }

    static CompositeConfiguration getConfigurationForConnection(DatabaseMetaData metaData) throws SQLException {
        String databaseName = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
        return getConfiguration(databaseName);
    }

    static String getRepoTableName(CompositeConfiguration configuration) {
        return configuration.getString("table.prefix") + "_" + configuration.getString("table.name");
    }

    public static String getRepoTableName(DatabaseMetaData metaData) throws SQLException {
        return getRepoTableName(getConfigurationForConnection(metaData));
    }
}
