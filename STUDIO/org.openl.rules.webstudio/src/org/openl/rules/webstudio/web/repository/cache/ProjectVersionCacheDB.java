package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.openl.rules.common.ProjectVersion;
import org.openl.util.db.SqlDBUtils;

public class ProjectVersionCacheDB extends H2CacheDB {

    public enum RepoType {
        DESIGN,
        DEPLOY
    }

    private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS VERSION_HASHES(project_name varchar(255),version varchar(255), created_at TIMESTAMP, created_by varchar(255), hash varchar(255), repository varchar(255))";
    private static final String SELECT_VERSION_QUERY = "select version from VERSION_HASHES WHERE created_at=(SELECT MAX(created_at) FROM VERSION_HASHES WHERE project_name=? and hash=? and repository=?)";
    private static final String SELECT_HASH_QUERY = "select hash from VERSION_HASHES WHERE created_at=(SELECT MAX(created_at) FROM VERSION_HASHES WHERE project_name=? and version=? and repository=?)";
    private static final String INSERT_QUERY = "INSERT INTO VERSION_HASHES" + "(project_name, version, created_at, created_by, hash, repository) values" + "(?,?,?,?,?,?)";
    private static final String SELECT_COUNT_QUERY = "select COUNT(*) from VERSION_HASHES";

    private static final String VERSION_FIELD = "version";
    private static final String HASH_FIELD = "hash";

    public ProjectVersionCacheDB() {
        super("projectsCache");
    }

    public String getHash(String name, String version, RepoType repoType) throws IOException {
        return getProjectData(name, version, repoType, HASH_FIELD, SELECT_HASH_QUERY);
    }

    public String getVersion(String name, String hash, RepoType repoType) throws IOException {
        return getProjectData(name, hash, repoType, VERSION_FIELD, SELECT_VERSION_QUERY);
    }

    public void insertProject(String projectName,
            ProjectVersion version,
            String hash,
            RepoType repoType) throws IOException {
        ensureCacheIsExist();
        Connection connection = null;
        PreparedStatement insertPreparedStatement = null;
        try {
            connection = getDBConnection();
            connection.setAutoCommit(false);
            insertPreparedStatement = connection.prepareStatement(INSERT_QUERY);
            insertPreparedStatement.setString(1, projectName);
            insertPreparedStatement.setString(2, version.getVersionName());
            insertPreparedStatement.setTimestamp(3, new java.sql.Timestamp(version.getVersionInfo().getCreatedAt().getTime()));
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

    private String getProjectData(String name,
            String field,
            RepoType repoType,
            String versionField,
            String query) throws IOException {
        ensureCacheIsExist();
        Connection connection = null;
        ResultSet rs = null;
        PreparedStatement selectPreparedStatement = null;
        try {
            connection = getDBConnection();
            selectPreparedStatement = connection.prepareStatement(query);
            selectPreparedStatement.setString(1, name);
            selectPreparedStatement.setString(2, field);
            selectPreparedStatement.setString(3, repoType.name());
            connection.setAutoCommit(false);
            rs = selectPreparedStatement.executeQuery();
            String version = null;
            while (rs.next()) {
                version = rs.getString(versionField);
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
