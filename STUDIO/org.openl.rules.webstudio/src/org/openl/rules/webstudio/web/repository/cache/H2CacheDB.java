package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;

public class H2CacheDB {

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:";
    private static final String CACHE_FOLDER = "/cache/";

    private static final JdbcDataSource jdbcDataSource = new JdbcDataSource();

    JdbcConnectionPool cp = JdbcConnectionPool.create(jdbcDataSource);

    private String cacheName = "";

    public H2CacheDB(String cacheName) {
        this.cacheName = cacheName;
    }

    protected Connection getDBConnection() throws IOException {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        try {
            return cp.getConnection();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public void setOpenLHome(String openLHome) {
        jdbcDataSource.setURL(DB_CONNECTION + openLHome + CACHE_FOLDER + cacheName);
    }

}
