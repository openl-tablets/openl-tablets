package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.util.db.SqlDBUtils;

public class ProjectVersionH2CacheDB extends H2CacheDB {

    public enum RepoType {
        DESIGN,
        DEPLOY
    }

    private static final String CACHE_NAME = "projectsCache";

    // cache table fields
    private static final String TABLE_NAME = "VERSION_HASHES";
    private static final int CACHE_VERSION = 1;
    private static final String PROJECT_NAME = "project_name";
    private static final String VERSION = "version";
    private static final String HASH = "hash";
    private static final String CREATED_AT = "created_at";
    private static final String CREATED_BY = "created_by";
    private static final String REPOSITORY = "repository";

    private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + PROJECT_NAME + " varchar(1000)," + VERSION + " varchar(50), " + CREATED_AT + " TIMESTAMP, " + CREATED_BY + " varchar(50), " + HASH + " varchar(32), " + REPOSITORY + " varchar(6))";
    private static final String SELECT_VERSION_QUERY = "SELECT " + VERSION + " FROM " + TABLE_NAME + " WHERE " + CREATED_AT + "=(SELECT MAX(" + CREATED_AT + ") FROM " + TABLE_NAME + " WHERE " + PROJECT_NAME + "=? AND " + HASH + "=? AND " + REPOSITORY + "=?) AND " + HASH + "=?";
    private static final String SELECT_HASH_QUERY = "SELECT " + HASH + " FROM " + TABLE_NAME + " WHERE " + CREATED_AT + "=? AND " + PROJECT_NAME + "=? AND " + REPOSITORY + "=? AND " + VERSION + "=?";
    private static final String SELECT_DESIGN_VERSION_QUERY = "select " + CREATED_AT + ", " + CREATED_BY + " FROM " + TABLE_NAME + " WHERE " + CREATED_AT + "=(SELECT MAX(" + CREATED_AT + ") FROM " + TABLE_NAME + " WHERE " + PROJECT_NAME + "=? AND " + HASH + "=? AND " + REPOSITORY + "=?)";
    private static final String INSERT_QUERY = "INSERT INTO " + TABLE_NAME + "(" + PROJECT_NAME + ", " + VERSION + ", " + CREATED_AT + ", " + CREATED_BY + ", " + HASH + ", " + REPOSITORY + ") values" + "(?,?,?,?,?,?)";
    private static final String SELECT_COUNT_QUERY = "SELECT COUNT(*) FROM " + TABLE_NAME;
    private static final String DROP_QUERY = "DROP TABLE IF EXISTS " + TABLE_NAME;

    private static final String STATE_TABLE_NAME = "CACHE_STATE";
    private static final String STATE = "state";

    private static final String CREATE_CACHE_STATE_QUERY = "CREATE TABLE IF NOT EXISTS " + STATE_TABLE_NAME + "(" + STATE + " BOOL," + VERSION + " INT)";
    private static final String INSERT_CACHE_STATE_QUERY = "INSERT INTO  " + STATE_TABLE_NAME + "(" + STATE + "," + VERSION + ") VALUES (FALSE, " + CACHE_VERSION + ")";
    private static final String UPDATE_CACHE_STATE_QUERY = "UPDATE " + STATE_TABLE_NAME + " SET " + STATE + " = ?";
    private static final String SELECT_CACHE_STATE_QUERY = "SELECT * FROM " + STATE_TABLE_NAME;
    private static final String DROP_STATE_QUERY = "DROP TABLE IF EXISTS " + STATE_TABLE_NAME;
    private static final String CHECK_STATE_QUERY = "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME ='" + STATE_TABLE_NAME + "'";

    public ProjectVersionH2CacheDB() {
        super(CACHE_NAME);
    }

    public void insertProject(String projectName,
            ProjectVersion version,
            String hash,
            RepoType repoType) throws IOException {
        Connection connection = null;
        PreparedStatement insertPreparedStatement = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            ensureCacheExist(connection);
            insertPreparedStatement = connection.prepareStatement(INSERT_QUERY);
            insertPreparedStatement.setString(1, projectName);
            insertPreparedStatement.setString(2, version.getVersionName());
            insertPreparedStatement.setTimestamp(3,
                new java.sql.Timestamp(version.getVersionInfo().getCreatedAt().getTime()));
            insertPreparedStatement.setString(4, version.getVersionInfo().getCreatedBy());
            insertPreparedStatement.setString(5, hash);
            insertPreparedStatement.setString(6, repoType.name());
            insertPreparedStatement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(insertPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
    }

    public boolean isCacheEmpty() throws IOException {
        int count = 0;
        Connection connection = null;
        PreparedStatement selectPreparedStatement = null;
        ResultSet rs = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            ensureCacheExist(connection);
            selectPreparedStatement = connection.prepareStatement(SELECT_COUNT_QUERY);
            rs = selectPreparedStatement.executeQuery();
            while (rs.next()) {
                count = rs.getInt(1);
            }
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

    public String getDesignBusinessVersion(String name, String hash, RepoType repoType) throws IOException {
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement selectPreparedStatement = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            ensureCacheExist(connection);
            selectPreparedStatement = connection.prepareStatement(SELECT_DESIGN_VERSION_QUERY);
            selectPreparedStatement.setString(1, name);
            selectPreparedStatement.setString(2, hash);
            selectPreparedStatement.setString(3, repoType.name());
            rs = selectPreparedStatement.executeQuery();
            Timestamp createdAt = null;
            String createdBy = null;
            while (rs.next()) {
                createdAt = rs.getTimestamp(CREATED_AT);
                createdBy = rs.getString(CREATED_BY);
            }
            String businessVersion = null;
            if (createdAt != null && createdBy != null) {
                String modifiedOnStr = WebStudioFormats.getInstance().formatDateTime(createdAt);
                businessVersion = createdBy + ": " + modifiedOnStr;
            }
            connection.commit();
            return businessVersion;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(selectPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
    }

    public String getHash(String name, String version, Date createdAt, RepoType repoType) throws IOException {
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement selectPreparedStatement = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            ensureCacheExist(connection);
            selectPreparedStatement = connection.prepareStatement(SELECT_HASH_QUERY);
            selectPreparedStatement.setTimestamp(1, new java.sql.Timestamp(createdAt.getTime()));
            selectPreparedStatement.setString(2, name);
            selectPreparedStatement.setString(3, repoType.name());
            selectPreparedStatement.setString(4, version);
            rs = selectPreparedStatement.executeQuery();
            String hash = null;
            while (rs.next()) {
                hash = rs.getString(HASH);
            }
            connection.commit();
            return hash;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(selectPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
    }

    public String getVersion(String name, String hash, RepoType repoType) throws IOException {
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement selectPreparedStatement = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            ensureCacheExist(connection);
            selectPreparedStatement = connection.prepareStatement(SELECT_VERSION_QUERY);
            selectPreparedStatement.setString(1, name);
            selectPreparedStatement.setString(2, hash);
            selectPreparedStatement.setString(3, repoType.name());
            selectPreparedStatement.setString(4, hash);
            rs = selectPreparedStatement.executeQuery();
            String version = null;
            while (rs.next()) {
                version = rs.getString(VERSION);
            }
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

    private void ensureCacheExist(Connection connection) throws IOException {
        PreparedStatement createPreparedStatement = null;
        try {
            createPreparedStatement = connection.prepareStatement(CREATE_QUERY);
            createPreparedStatement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(createPreparedStatement);
        }
    }

    public void setCacheCalculatedState(boolean state) throws IOException {
        Connection connection = null;
        PreparedStatement updatePreparedStatement = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            ensureCacheStateExist(connection);
            updatePreparedStatement = connection.prepareStatement(UPDATE_CACHE_STATE_QUERY);
            updatePreparedStatement.setBoolean(1, state);
            updatePreparedStatement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(updatePreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
    }

    public boolean isCacheCalculated() throws IOException {
        Connection connection = null;
        PreparedStatement selectPreparedStatement = null;
        ResultSet rs = null;
        boolean state = false;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            ensureCacheStateExist(connection);
            selectPreparedStatement = connection.prepareStatement(SELECT_CACHE_STATE_QUERY);
            rs = selectPreparedStatement.executeQuery();
            while (rs.next()) {
                state = rs.getBoolean(STATE);
            }
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(selectPreparedStatement);
            SqlDBUtils.safeClose(connection);
        }
        return state;
    }

    public void closeDb() throws IOException {
        Connection connection = null;
        try {
            connection = getDBConnection();
            connection.createStatement().execute("SHUTDOWN");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void recreateCache(Connection connection) throws IOException {
        PreparedStatement insertPreparedStatement = null;
        PreparedStatement dropPreparedStatement = null;
        PreparedStatement createPreparedStatement = null;
        try {
            dropPreparedStatement = connection.prepareStatement(DROP_QUERY);
            dropPreparedStatement.execute();
            dropPreparedStatement = connection.prepareStatement(DROP_STATE_QUERY);
            dropPreparedStatement.execute();
            ensureCacheExist(connection);
            createPreparedStatement = connection.prepareStatement(CREATE_CACHE_STATE_QUERY);
            createPreparedStatement.executeUpdate();
            insertPreparedStatement = connection.prepareStatement(INSERT_CACHE_STATE_QUERY);
            insertPreparedStatement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(insertPreparedStatement);
            SqlDBUtils.safeClose(dropPreparedStatement);
            SqlDBUtils.safeClose(createPreparedStatement);
        }
    }

    private void ensureCacheStateExist(Connection connection) throws IOException {
        PreparedStatement checkPreparedStatement = null;
        PreparedStatement selectPreparedStatement = null;
        PreparedStatement insertPreparedStatement = null;
        ResultSet rs = null;
        int state = 0;
        try {
            connection.setAutoCommit(false);
            checkPreparedStatement = connection.prepareStatement(CHECK_STATE_QUERY);
            ResultSet resultSet = checkPreparedStatement.executeQuery();
            if (resultSet.next()) {
                selectPreparedStatement = connection.prepareStatement(SELECT_CACHE_STATE_QUERY);
                rs = selectPreparedStatement.executeQuery();
                while (rs.next()) {
                    state = rs.getInt(VERSION);
                }
                if (state == 0) {
                    insertPreparedStatement = connection.prepareStatement(INSERT_CACHE_STATE_QUERY);
                    insertPreparedStatement.executeUpdate();
                } else if (state != CACHE_VERSION) {
                    recreateCache(connection);
                }
            } else {
                recreateCache(connection);
            }
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(checkPreparedStatement);
            SqlDBUtils.safeClose(selectPreparedStatement);
            SqlDBUtils.safeClose(insertPreparedStatement);
            SqlDBUtils.safeClose(rs);
        }
    }
}
