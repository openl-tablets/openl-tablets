package org.openl.rules.repository.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import lombok.Setter;

import org.openl.util.StringUtils;

public class JdbcDBRepository extends DBRepository {
    @Setter
    private String uri;
    @Setter
    private String login;
    @Setter
    private String password;

    @Override
    protected Connection createConnection() throws SQLException {
        if (StringUtils.isBlank(login)) {
            return DriverManager.getConnection(uri);
        } else {
            return DriverManager.getConnection(uri, login, password);
        }
    }
}
