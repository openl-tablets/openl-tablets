package org.openl.rules.repository.factories;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DBConfigurationLoader {
    private DBConfigurationLoader() {
    }

    private static CompositeConfiguration getConfiguration(String databaseName) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(createConfiguration("modeshape-" + databaseName + ".properties"));
        compositeConfiguration.addConfiguration(createConfiguration("modeshape.properties"));
        return compositeConfiguration;
    }

    private static PropertiesConfiguration createConfiguration(String configLocation) {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setDelimiterParsingDisabled(true);
        configuration.setFileName(configLocation);
        try {
            configuration.load();
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            Logger logger = LoggerFactory.getLogger(DBRepositoryFactory.class);
            logger.error("Error when initializing configuration: {}", configLocation, e);
        }
        return configuration;
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
