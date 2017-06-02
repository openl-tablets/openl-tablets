package org.openl.rules.db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBMigrationBean {
    private final Logger log = LoggerFactory.getLogger(DBMigrationBean.class);

    private DataSource dataSource;

    public void init() throws Exception {
        Connection connection = dataSource.getConnection();
        String databaseCode;
        boolean oldMigrationExists = false;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
            String tableName = "schema_version";
            String repoTable = metaData.storesUpperCaseIdentifiers() ? tableName.toUpperCase() : tableName;

            ResultSet rs = null;
            try {
                if ("oracle".equals(databaseCode)) {
                    rs = metaData.getTables(null, metaData.getUserName(), repoTable, new String[] { "TABLE" });
                } else {
                    rs = metaData.getTables(null, null, repoTable, new String[] { "TABLE" });
                }
                oldMigrationExists = rs.next();
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        log.warn("Unexpected sql failure", e);
                    }
                }
            }
        } finally {
            connection.close();
        }

        if (oldMigrationExists) {
            throw new IllegalStateException("Incompatible OpenL WebStudio version");
        }
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
