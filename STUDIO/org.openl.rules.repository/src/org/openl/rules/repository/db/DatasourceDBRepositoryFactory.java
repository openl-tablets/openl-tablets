package org.openl.rules.repository.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.openl.util.StringUtils;

public class DatasourceDBRepositoryFactory extends DBRepository {

    private DataSource dataSource;
    private String uri;
    private String login;
    private String password;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
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

    @Override
    public void initialize() {
        init();
        super.initialize();
    }

    private void init() {
        if (dataSource != null) {
            return;
        }
        if (StringUtils.isBlank(uri)) {
            throw new IllegalStateException("Required 'uri' property is not defined.");
        }
        InitialContext initialContext = createInitialContext();

        RuntimeException exception = null;
        try {
            this.dataSource = (DataSource) initialContext.lookup(uri);
        } catch (Throwable e) {
            exception = new IllegalStateException("Cannot determine JNDI [ " + uri + " ] name", e);
            throw exception;
        } finally {
            if (exception != null) {
                try {
                    initialContext.close();
                } catch (Throwable e) {
                    exception.addSuppressed(new IllegalStateException("Cannot close JNDI context", e));
                }
            } else {
                closeInitialContext(initialContext);
            }
        }
        if (dataSource == null) {
            throw new IllegalStateException("DataSource has not been found in JNDI context by 'uri' : " + uri);
        }
    }

    private InitialContext createInitialContext() {
        try {
            return new InitialContext();
        } catch (NamingException e) {
            throw new IllegalStateException("Cannot initialize JNDI context", e);
        }
    }

    private void closeInitialContext(InitialContext initialContext) {
        try {
            initialContext.close();
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot close JNDI context", e);
        }
    }
}
