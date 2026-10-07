package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;

import org.openl.util.FileUtils;

@RequiredArgsConstructor
public class H2CacheDB {

    private static final String DB_CONNECTION = "jdbc:h2:";
    private static final String CACHE_FOLDER = "/cache/";

    private static final JdbcDataSource jdbcDataSource = new JdbcDataSource();

    JdbcConnectionPool cp = JdbcConnectionPool.create(jdbcDataSource);

    private final String cacheName;
    private boolean initialized;

    protected Connection getDBConnection() throws IOException {
        try {
            var connection = cp.getConnection();
            initialized = true;
            return connection;
        } catch (SQLException e) {
            if (!initialized) {
                // Probably db file is broken or unsupported version. Clear it so it will be recreated again.
                var dbPath = jdbcDataSource.getURL().substring(DB_CONNECTION.length());
                FileUtils.deleteQuietly(Path.of(dbPath + ".mv.db"));
                try {
                    var connection = cp.getConnection();
                    initialized = true;
                    return connection;
                } catch (SQLException ex) {
                    throw new IOException(ex);
                }
            }
            throw new IOException(e);
        }
    }

    public void setOpenLHome(String openLHome) {
        jdbcDataSource.setURL(DB_CONNECTION + openLHome + CACHE_FOLDER + cacheName);
    }

}
