package org.openl.rules.repository.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JdbcDBRepositoryFactory extends BaseDBRepositoryFactory {
    protected final String uri;
    protected final String login;
    protected final String password;

    public JdbcDBRepositoryFactory(String uri, String login, String password, boolean designMode) {
        this.uri = uri;
        this.login = login;
        this.password = password;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(uri, login, password);
    }
}
