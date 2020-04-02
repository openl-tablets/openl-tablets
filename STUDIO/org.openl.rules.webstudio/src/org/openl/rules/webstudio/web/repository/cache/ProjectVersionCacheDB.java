package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.util.db.SqlDBUtils;

public class ProjectVersionCacheDB extends H2CacheDB {

    public enum RepoType {
        DESIGN,
        DEPLOY
    }

    private static final String CACHE_NAME = "projectsCache";

    // cache table fields
    private static final String TABLE_NAME = "VERSION_HASHES";
    private static final String PROJECT_NAME = "project_name";
    private static final String VERSION = "version";
    private static final String HASH = "hash";
    private static final String CREATED_AT = "created_at";
    private static final String CREATED_BY = "created_by";
    private static final String REPOSITORY = "repository";

    private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + PROJECT_NAME + " varchar(300)," + VERSION + " varchar(50), " + CREATED_AT + " TIMESTAMP, " + CREATED_BY + " varchar(50), " + HASH + " varchar(32), " + REPOSITORY + " varchar(6))";
    private static final String SELECT_VERSION_QUERY = "select " + VERSION + " from " + TABLE_NAME + " WHERE " + CREATED_AT + "=(SELECT MAX(" + CREATED_AT + ") FROM " + TABLE_NAME + " WHERE " + PROJECT_NAME + "=? and " + HASH + "=? and " + REPOSITORY + "=?) and " + HASH + "=?";
    private static final String SELECT_HASH_QUERY = "select " + HASH + " from " + TABLE_NAME + " WHERE " + CREATED_AT + "=(SELECT MAX(" + CREATED_AT + ") FROM " + TABLE_NAME + " WHERE " + PROJECT_NAME + "=? and " + VERSION + "=? and " + REPOSITORY + "=?) and " + HASH + "=?";
    private static final String SELECT_DESIGN_VERSION_QUERY = "select " + CREATED_AT + ", " + CREATED_BY + " from " + TABLE_NAME + " WHERE " + CREATED_AT + "=(SELECT MAX(" + CREATED_AT + ") FROM " + TABLE_NAME + " WHERE " + PROJECT_NAME + "=? and " + HASH + "=? and " + REPOSITORY + "=?)";
    private static final String INSERT_QUERY = "INSERT INTO " + TABLE_NAME + "" + "(" + PROJECT_NAME + ", " + VERSION + ", " + CREATED_AT + ", " + CREATED_BY + ", " + HASH + ", " + REPOSITORY + ") values" + "(?,?,?,?,?,?)";
    private static final String SELECT_COUNT_QUERY = "select COUNT(*) from " + TABLE_NAME;

    public ProjectVersionCacheDB() {
        super(CACHE_NAME);
    }

    public String getHash(String name, String version, RepoType repoType) throws IOException {
        return getProjectData(name, version, repoType, HASH, SELECT_HASH_QUERY);
    }

    public String getVersion(String name, String hash, RepoType repoType) throws IOException {
        return getProjectData(name, hash, repoType, VERSION, SELECT_VERSION_QUERY);
    }

    public void insertProject(String projectName,
            ProjectVersion version,
            String hash,
            RepoType repoType) throws IOException {
        Connection connection = null;
        PreparedStatement insertPreparedStatement = null;
        try {
            connection = getDBConnection();
            ensureCacheIsExist(connection);
            connection.setAutoCommit(false);
            insertPreparedStatement = connection.prepareStatement(INSERT_QUERY);
            insertPreparedStatement.setString(1, projectName);
            insertPreparedStatement.setString(2, version.getVersionName());
            insertPreparedStatement.setTimestamp(3,
                new java.sql.Timestamp(version.getVersionInfo().getCreatedAt().getTime()));
            insertPreparedStatement.setString(4, version.getVersionInfo().getCreatedBy());
            insertPreparedStatement.setString(5, hash);
            insertPreparedStatement.setString(6, repoType.name());
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
        int count = 0;
        Connection connection = null;
        PreparedStatement selectPreparedStatement = null;
        ResultSet rs = null;
        try {
            connection = getDBConnection();
            ensureCacheIsExist(connection);
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

    public String getDesignBusinessVersion(String name, String hash, RepoType repoType) throws IOException {
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement selectPreparedStatement = null;
        try {
            connection = getDBConnection();
            ensureCacheIsExist(connection);
            selectPreparedStatement = connection.prepareStatement(SELECT_DESIGN_VERSION_QUERY);
            selectPreparedStatement.setString(1, name);
            selectPreparedStatement.setString(2, hash);
            selectPreparedStatement.setString(3, repoType.name());
            connection.setAutoCommit(false);
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
            selectPreparedStatement.close();
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

    private void ensureCacheIsExist(Connection connection) throws IOException {
        PreparedStatement createPreparedStatement = null;
        try {
            connection.setAutoCommit(false);
            createPreparedStatement = connection.prepareStatement(CREATE_QUERY);
            createPreparedStatement.executeUpdate();
            createPreparedStatement.close();
            connection.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(createPreparedStatement);
        }
    }

    private String getProjectData(String name,
            String field,
            RepoType repoType,
            String resultField,
            String query) throws IOException {
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement selectPreparedStatement = null;
        try {
            connection = getDBConnection();
            ensureCacheIsExist(connection);
            selectPreparedStatement = connection.prepareStatement(query);
            selectPreparedStatement.setString(1, name);
            selectPreparedStatement.setString(2, field);
            selectPreparedStatement.setString(3, repoType.name());
            selectPreparedStatement.setString(4, field);
            connection.setAutoCommit(false);
            rs = selectPreparedStatement.executeQuery();
            String version = null;
            while (rs.next()) {
                version = rs.getString(resultField);
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

}
