package org.openl.rules.db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBMigrationBean {
    private final Logger log = LoggerFactory.getLogger(DBMigrationBean.class);

    private String additionalMigrationPaths;

    private DataSource dataSource;

    public void init() throws Exception {
        Connection connection = dataSource.getConnection();
        String databaseCode;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
        } finally {
            connection.close();
        }

        ArrayList<String> locations = new ArrayList<String>();
        locations.add("/db/migration/common");
        locations.add("/db/migration/" + databaseCode);

        TreeMap<String, String> placeholders = new TreeMap<String, String>();
        for (String location : locations) {
            fillQueries(placeholders, location + "/placeholders.properties");
        }

        // Additional migrations
        if (StringUtils.isNotBlank(additionalMigrationPaths)) {
            String[] split = StringUtils.split(additionalMigrationPaths, ',');
            locations.addAll(Arrays.asList(split));
        }

        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setPlaceholders(placeholders);
        flyway.setLocations(locations.toArray(new String[0]));
        flyway.setBaselineVersionAsString("0");
        flyway.setBaselineOnMigrate(true);
        flyway.migrate();
    }

    public void setAdditionalMigrationPaths(String additionalMigrationPaths) {
        this.additionalMigrationPaths = additionalMigrationPaths;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void fillQueries(Map<String, String> queries, String propertiesFileName) throws IOException {
        URL resource = getClass().getResource(propertiesFileName);
        if (resource == null) {
            log.info("File '{}' is not found.", propertiesFileName);
            return;
        }
        log.info("Load properties from '{}'.", resource);
        InputStream is = resource.openStream();
        try {
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                queries.put(key, properties.getProperty(key));
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
