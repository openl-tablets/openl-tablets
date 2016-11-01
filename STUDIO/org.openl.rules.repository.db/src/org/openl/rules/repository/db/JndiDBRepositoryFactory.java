package org.openl.rules.repository.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class JndiDBRepositoryFactory extends BaseDBRepositoryFactory {
    public JndiDBRepositoryFactory(String uri, String login, String password) {
        super(uri, login, password);
    }

    @Override
    protected Connection createConnection(String url, String user, String password) {
        try {
            DataSource datasource = (DataSource) new InitialContext().lookup(url);
            return datasource.getConnection();
        } catch (NamingException e) {
            throw new IllegalStateException("Cannot determine JNDI [ " + url + " ] name", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create a connection using JNDI [ " + url + " ] name", e);
        }
    }
}
