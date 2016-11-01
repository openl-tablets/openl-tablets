package org.openl.rules.repository.factories;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.infinispan.loaders.jdbc.configuration.JdbcStringBasedCacheStoreConfigurationBuilder;

/**
 * Allows to connect DB repository using a shared DataSource from JNDI.
 *
 * @author Yury Molchan
 */
public class JndiDBRepositoryFactory extends DBRepositoryFactory {

    public JndiDBRepositoryFactory(String uri, String login, String password, boolean designMode) {
        super(uri, login, password, designMode);
    }

    Connection createConnection(String url, String user, String password) {
        try {
            DataSource datasource = (DataSource) new InitialContext().lookup(url);
            Connection conn = datasource.getConnection();
            return conn;
        } catch (NamingException e) {
            throw new IllegalStateException("Cannot determine JNDI [ " + url + " ] name", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create a connection using JNDI [ " + url + " ] name", e);
        }
    }

    void buildDBConnection(JdbcStringBasedCacheStoreConfigurationBuilder jdbcBuilder,
            String url,
            String user,
            String password) {

        jdbcBuilder.dataSource().jndiUrl(url);
    }
}
