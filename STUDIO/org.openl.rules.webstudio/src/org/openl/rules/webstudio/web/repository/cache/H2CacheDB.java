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

    public enum RepoType {
        DESIGN,
        DEPLOY
    }

    private final Logger log = LoggerFactory.getLogger(H2CacheDB.class);

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:";
    private static final String CACHE_NAME = "/cache/hashProjectCache";
    private String openLHome = "";

    private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS VERSION_HASHES(project_name varchar(255),version varchar(255), hash varchar(255), repository varchar(255))";
    private static final String SELECT_VERSION_QUERY = "select version from VERSION_HASHES WHERE project_name=? and hash=? and repository=?";
    private static final String SELECT_HASH_QUERY = "select hash from VERSION_HASHES WHERE project_name=? and version=? and repository=?";
    private static final String INSERT_QUERY = "INSERT INTO VERSION_HASHES" + "(project_name, version, hash, repository) values" + "(?,?,?,?)";
    private static final String SELECT_COUNT_QUERY = "select COUNT(*) from VERSION_HASHES";

    private static final String VERSION_FIELD = "version";
    private static final String HASH_FIELD = "hash";

    public String getHash(String name, String version, RepoType repoType) throws IOException {
        return getProjectData(name, version, repoType, SELECT_HASH_QUERY, HASH_FIELD);
    }

    public String getVersion(String name, String hash, RepoType repoType) throws IOException {
        return getProjectData(name, hash, repoType, SELECT_VERSION_QUERY, VERSION_FIELD);
    }

    private String getProjectData(String name, String param, RepoType repoType, String selectQuery, String field) throws IOException {
        ensureCacheIsExist();
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement selectPreparedStatement = null;
        try {
            connection = getDBConnection();
            selectPreparedStatement = connection.prepareStatement(selectQuery);
            selectPreparedStatement.setString(1, name);
            selectPreparedStatement.setString(2, param);
            selectPreparedStatement.setString(3, repoType.name());
            connection.setAutoCommit(false);
            rs = selectPreparedStatement.executeQuery();
            String result = null;
            while (rs.next()) {
                result = rs.getString(field);
            }
            selectPreparedStatement.close();
            connection.commit();
            return result;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(selectPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
    }

    public void insert(String projectName, String version, String hash, RepoType repoType) throws IOException {
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
            insertPreparedStatement.setString(4, repoType.name());
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
            dbConnection = DriverManager.getConnection(DB_CONNECTION + openLHome + CACHE_NAME, "", "");
            return dbConnection;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public void setOpenLHome(String openLHome) {
        this.openLHome = openLHome;
    }
}
