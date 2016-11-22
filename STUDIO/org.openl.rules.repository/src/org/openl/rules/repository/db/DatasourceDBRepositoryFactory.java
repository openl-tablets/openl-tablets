package org.openl.rules.repository.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.openl.util.StringUtils;

public class DatasourceDBRepositoryFactory extends BaseDBRepositoryFactory {
    private final String login;
    private final String password;
    protected DataSource dataSource;

    public DatasourceDBRepositoryFactory(DataSource dataSource) {
        this(dataSource, null, null);
    }

    public DatasourceDBRepositoryFactory(DataSource dataSource, String login, String password) {
        this.dataSource = dataSource;
        this.login = login;
        this.password = password;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        if (StringUtils.isBlank(login)) {
            return dataSource.getConnection();
        } else {
            return dataSource.getConnection(login, password);
        }
    }

}
