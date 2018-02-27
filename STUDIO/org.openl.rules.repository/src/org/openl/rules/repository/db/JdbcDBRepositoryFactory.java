package org.openl.rules.repository.db;

import org.openl.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JdbcDBRepositoryFactory extends DBRepository {
    private String uri;
    private String login;
    private String password;

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        if (StringUtils.isBlank(login)) {
            Connection conn = DriverManager.getConnection(uri);
            conn.setAutoCommit(false);
            return conn;
        } else {
            Connection conn = DriverManager.getConnection(uri, login, password);
            conn.setAutoCommit(false);
            return conn;
        }
    }
}
