package org.openl.rules.repository.db;

import java.io.IOException;
import java.sql.*;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDBRepositoryFactory extends DBRepository implements RRepositoryFactory {
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

            initializeTable(connection, databaseType);
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
        super.close();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }

    private static void initializeTable(Connection connection, DatabaseType databaseType) throws SQLException {
        if (!tableExists(connection, databaseType)) {
            switch (databaseType) {
                case H2:
                    createTable(connection, DatabaseQueries.H2_TABLE);
                    break;
                case MYSQL:
                    createTable(connection, DatabaseQueries.MYSQL_TABLE);
                    break;
                case POSTGRESQL:
                    createTable(connection, DatabaseQueries.POSTGRESQL_TABLE);
                    break;
                case ORACLE:
                    createTable(connection, DatabaseQueries.ORACLE_TABLE, DatabaseQueries.ORACLE_SEQUENCE, DatabaseQueries.ORACLE_TRIGGER);
                    break;
                case SQL_SERVER:
                    createTable(connection, DatabaseQueries.SQLSERVER_TABLE);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported database " + connection.getMetaData().getDatabaseProductName());
            }
        }
    }

    private static void createTable(Connection connection, String... queries) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            for (String query : queries) {
                statement.execute(query);
            }
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    Logger log = LoggerFactory.getLogger(DatabaseQueries.class);
                    log.warn("Unexpected sql failure", e);
                }
            }
        }

    }

    private static boolean tableExists(Connection connection, DatabaseType databaseType) throws SQLException {
        ResultSet rs = null;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String repoTable = metaData.storesUpperCaseIdentifiers() ? DatabaseQueries.REPOSITORY_NAME.toUpperCase() : DatabaseQueries.REPOSITORY_NAME;
            switch (databaseType) {
                case ORACLE:
                    rs = metaData.getTables(null, metaData.getUserName(), repoTable, new String[] { "TABLE" });
                    break;
                default:
                    rs = metaData.getTables(null, null, repoTable, new String[] { "TABLE" });
            }

            return rs.next();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    Logger log = LoggerFactory.getLogger(DatabaseQueries.class);
                    log.warn("Unexpected sql failure", e);
                }
            }
        }
    }
}
