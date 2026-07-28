package org.openl.rules.security.standalone;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

import org.openl.util.PropertiesUtils;

@Slf4j
public class DBMigrationBean {

    private DataSource dataSource;

    public void init() throws SQLException, IOException {

        String databaseCode;
        try (var connection = dataSource.getConnection()) {
            var metaData = connection.getMetaData();
            databaseCode = metaData.getDatabaseProductName().toLowerCase(Locale.ROOT).replace(" ", "_");
        }

        String[] locations = {"/db/flyway/common", "/db/flyway/" + databaseCode};

        var placeholders = new TreeMap<String, String>();
        for (String location : locations) {
            fillQueries(placeholders, location + "/placeholders.properties");
        }
        var flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setBaselineVersionAsString("0");
        flyway.setBaselineOnMigrate(true);
        // Tolerate cosmetic changes (for example, tabs reformatted to spaces) to already-applied
        // migration scripts: such edits change the Flyway checksum but not the SQL. Strict
        // validation would otherwise block the upgrade, and the bundled Flyway 4.2 repair() is
        // incompatible with the embedded H2. New migrations are still applied by version.
        flyway.setValidateOnMigrate(false);
        flyway.setTable("openl_security_flyway");
        flyway.setPlaceholders(placeholders);

        flyway.setLocations(locations);
        flyway.migrate();
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void fillQueries(Map<String, String> queries, String propertiesFileName) throws IOException {
        var resource = getClass().getResource(propertiesFileName);
        if (resource == null) {
            log.info("File '{}' is not found.", propertiesFileName);
            return;
        }
        log.info("Load properties from '{}'.", resource);
        PropertiesUtils.load(resource, queries::put);
    }
}
