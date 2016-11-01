package org.openl.rules.repository.db;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDBRepositoryFactory extends DBRepository implements RRepositoryFactory, Closeable {
    private final Logger log = LoggerFactory.getLogger(JdbcDBRepositoryFactory.class);

    protected final String uri;
    protected final String login;
    protected final String password;
    private Connection connection;

    public BaseDBRepositoryFactory(String uri, String login, String password) {
        this.uri = uri;
        this.login = login;
        this.password = password;
    }

    @Override
    public void initialize() throws RRepositoryException {
        registerDrivers();
        Connection connection = createConnection(uri, login, password);
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            DatabaseType databaseType = DatabaseType.fromString(metaData.getDatabaseProductName()
                    .toLowerCase()
                    .replace(" ", "_"));

            DatabaseQueries.initializeTable(connection, databaseType);
        } catch (SQLException e) {
            throw new RRepositoryException("Can't initialize repository", e);
        }
        this.connection = connection;
    }

    protected abstract Connection createConnection(String url, String user, String password);

    private void registerDrivers() {
        // Defaults drivers
        String[] drivers = { "com.mysql.jdbc.Driver",
                "com.ibm.db2.jcc.DB2Driver",
                "oracle.jdbc.OracleDriver",
                "org.postgresql.Driver",
                "org.hsqldb.jdbcDriver",
                "org.h2.Driver",
                "com.microsoft.sqlserver.jdbc.SQLServerDriver" };
        registerDrivers(drivers);
        drivers = StringUtils.split(System.getProperty("jdbc.drivers"), ':');
        registerDrivers(drivers);
    }

    private void registerDrivers(String... drivers) {
        if (drivers == null) {
            return;
        }
        for (String driver : drivers) {
            try {
                Class.forName(driver);
                log.info("JDBC Driver: '{}' - OK.", driver);
            } catch (ClassNotFoundException e) {
                log.info("JDBC Driver: '{}' - NOT FOUND.", driver);
            }
        }
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }

    @Override
    public void close() throws IOException {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }
}
