package org.openl.rules.repository.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JdbcDBRepositoryFactory extends BaseDBRepositoryFactory {

    public JdbcDBRepositoryFactory(String uri, String login, String password, boolean designMode) {
        super(uri, login, password);
    }

    @Override
    protected Connection createConnection(String url, String user, String password) {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create a connection to [ " + url + " ] URL", e);
        }
    }
}
