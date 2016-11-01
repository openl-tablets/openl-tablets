package org.openl.rules.repository.factories;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.infinispan.loaders.jdbc.configuration.JdbcStringBasedCacheStoreConfigurationBuilder;

/**
 * Allows to connect DB repository via JDBC driver.
 *
 * @author Yury Molchan
 */
public class JdbcDBRepositoryFactory extends DBRepositoryFactory {

    public JdbcDBRepositoryFactory(String uri, String login, String password, boolean designMode) {
        super(uri, login, password, designMode);
    }

    Connection createConnection(String url, String user, String password) {
        Connection conn;
        try {
            conn = DriverManager.getConnection(url, user, password);
            return conn;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create a connection to [ " + url + " ] URL", e);
        }
    }

    void buildDBConnection(JdbcStringBasedCacheStoreConfigurationBuilder jdbcBuilder,
            String url,
            String user,
            String password) {

        Driver driver;
        try {
            driver = DriverManager.getDriver(url);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot determine the driver by [ " + url + " ] URL", e);
        }

        String driverClass = driver.getClass().getName();
        jdbcBuilder.connectionPool()
            .connectionUrl(url)
            .username(user)
            .password(password)
            // Get a driver by url
            .driverClass(driverClass);

    }
}
