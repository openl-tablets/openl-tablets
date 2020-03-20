package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.openl.util.db.SqlDBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2CacheDB {

    private final Logger log = LoggerFactory.getLogger(H2CacheDB.class);

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:~/test2";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";

    private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS VERSION_HASHES(project_name varchar(255),version varchar(255), hash varchar(255))";
    private static final String SELECT_QUERY = "select version from VERSION_HASHES WHERE hash=?";
    private static final String INSERT_QUERY = "INSERT INTO VERSION_HASHES" + "(project_name, version, hash) values" + "(?,?,?)";
    private static final String SELECT_COUNT_QUERY = "select COUNT(*) from VERSION_HASHES";

    private static final String VERSION_FIELD = "version";

    public String checkHash(String hash) throws IOException {
        ensureCacheIsExist();
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement selectPreparedStatement = null;
        try {
            connection = getDBConnection();
            selectPreparedStatement = connection.prepareStatement(SELECT_QUERY);
            selectPreparedStatement.setString(1, hash);
            connection.setAutoCommit(false);
            rs = selectPreparedStatement.executeQuery();
            selectPreparedStatement = connection.prepareStatement(SELECT_QUERY);
            String version = null;
            while (rs.next()) {
                version = rs.getString(VERSION_FIELD);
            }
            selectPreparedStatement.close();
            connection.commit();
            return version;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(selectPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
    }

    public void insert(String projectName, String version, String hash) throws IOException {
        ensureCacheIsExist();
        Connection connection = null;
        PreparedStatement insertPreparedStatement = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            insertPreparedStatement = connection.prepareStatement(INSERT_QUERY);
            insertPreparedStatement.setString(1, projectName);
            insertPreparedStatement.setString(2, version);
            insertPreparedStatement.setString(3, hash);
            insertPreparedStatement.executeUpdate();
            insertPreparedStatement.close();
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(insertPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
    }

    public boolean isCacheEmpty() throws IOException {
        ensureCacheIsExist();
        int count = 0;
        Connection connection = null;
        PreparedStatement selectPreparedStatement = null;
        ResultSet rs = null;

        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            selectPreparedStatement = connection.prepareStatement(SELECT_COUNT_QUERY);
            rs = selectPreparedStatement.executeQuery();
            while (rs.next()) {
                count = rs.getInt(1);
            }
            selectPreparedStatement.close();

            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(selectPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
        return count == 0;
    }

    private void ensureCacheIsExist() throws IOException {
        Connection connection = null;
        PreparedStatement createPreparedStatement = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            createPreparedStatement = connection.prepareStatement(CREATE_QUERY);
            createPreparedStatement.executeUpdate();
            createPreparedStatement.close();
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(createPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
    }

    private Connection getDBConnection() throws IOException {
        Connection dbConnection = null;
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
            return dbConnection;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
