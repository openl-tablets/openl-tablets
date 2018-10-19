package org.openl.rules.repository.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.common.RevisionGetter;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.db.JDBCDriverRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DBRepository implements Repository, Closeable, RRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(DBRepository.class);

    private Settings settings;
    private ChangesMonitor monitor;

    @Override
    public List<FileData> list(String path) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.selectAllMetainfo);
            statement.setString(1, makePathPattern(path));
            rs = statement.executeQuery();

            List<FileData> fileDatas = new ArrayList<FileData>();
            while (rs.next()) {
                FileData fileData = createFileData(rs);
                fileDatas.add(fileData);
            }

            rs.close();
            statement.close();

            return fileDatas;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public FileData check(String name) throws IOException {
        return getLatestVersionFileData(name);
    }

    @Override
    public FileItem read(String name) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.readActualFile);
            statement.setString(1, name);
            rs = statement.executeQuery();

            FileItem fileItem = null;
            if (rs.next()) {
                fileItem = createFileItem(rs);
            }

            rs.close();
            statement.close();

            return fileItem;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        return insertFile(data, stream);
    }

    @Override
    public boolean delete(FileData path) {
        FileData data;
        try {
            data = getLatestVersionFileData(path.getName());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }

        if (data != null) {
            try {
                insertFile(data, null);
                invokeListener();
                return true;
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public FileData copy(String srcName, FileData destData) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.copyFile);
            statement.setString(1, destData.getName());
            statement.setString(2, destData.getAuthor());
            statement.setString(3, destData.getComment());

            statement.setString(4, srcName);
            statement.executeUpdate();

            invokeListener();
            return destData;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public FileData rename(String path, FileData destData) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setListener(final Listener callback) {
        monitor.setListener(callback);
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.selectAllHistoryMetainfo);
            statement.setString(1, name);
            rs = statement.executeQuery();

            List<FileData> fileDatas = new ArrayList<FileData>();
            while (rs.next()) {
                FileData fileData = createFileData(rs);
                fileDatas.add(fileData);
            }

            rs.close();
            statement.close();

            return fileDatas;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        return version == null ? check(name) : getHistoryVersionFileData(name, version);
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.readHistoricFile);
            statement.setLong(1, Long.valueOf(version));
            statement.setString(2, name);
            rs = statement.executeQuery();

            FileItem fileItem = null;
            if (rs.next()) {
                fileItem = createFileItem(rs);
            }

            rs.close();
            statement.close();

            return fileItem;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public boolean deleteHistory(String name, String version) {
        if (version == null) {
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = getConnection();
                statement = connection.prepareStatement(settings.deleteAllHistory);
                statement.setString(1, name);
                int rows = statement.executeUpdate();

                if (rows > 0) {
                    invokeListener();
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                return false;
            } finally {
                safeClose(statement);
                safeClose(connection);
            }
        } else {
            Connection connection = null;
            PreparedStatement statement = null;
            try {
                connection = getConnection();
                statement = connection.prepareStatement(settings.deleteVersion);
                statement.setLong(1, Long.valueOf(version));
                statement.setString(2, name);
                int rows = statement.executeUpdate();

                if (rows > 0) {
                    invokeListener();
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                return false;
            } finally {
                safeClose(statement);
                safeClose(connection);
            }
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        if (version == null) {
            return copy(srcName, destData);
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.copyHistory);
            statement.setString(1, destData.getName());
            statement.setString(2, destData.getAuthor());
            statement.setString(3, destData.getComment());

            statement.setLong(4, Long.valueOf(version));
            statement.setString(5, srcName);
            statement.executeUpdate();

            invokeListener();
            return destData;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    protected abstract Connection getConnection() throws SQLException;

    private FileData getLatestVersionFileData(String name) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.readActualFileMetainfo);
            statement.setString(1, name);
            rs = statement.executeQuery();

            FileData fileData = null;
            if (rs.next()) {
                fileData = createFileData(rs);
            }

            rs.close();
            statement.close();

            return fileData;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
            safeClose(connection);
        }
    }

    private FileData getHistoryVersionFileData(String name, String version) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.readHistoricFileMetainfo);
            statement.setLong(1, Long.valueOf(version));
            statement.setString(2, name);
            rs = statement.executeQuery();

            FileData fileData = null;
            if (rs.next()) {
                fileData = createFileData(rs);
            }

            rs.close();
            statement.close();

            return fileData;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
            safeClose(connection);
        }
    }

    private FileItem createFileItem(ResultSet rs) throws SQLException {
        FileData fileData = createFileData(rs);
        InputStream data = rs.getBinaryStream("file_data");
        if (data == null) {
            return null;
        }

        // ResultSet will be closed, so InputStream can be closed too, that's
        // why copy it to byte array before.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(data, out);
        } catch (IOException e) {
            throw new SQLException(e);
        }
        return new FileItem(fileData, new ByteArrayInputStream(out.toByteArray()));
    }

    private FileData createFileData(ResultSet rs) throws SQLException {
        FileData fileData = new FileData();
        fileData.setName(rs.getString("file_name"));
        fileData.setSize(rs.getLong("file_size"));
        fileData.setAuthor(rs.getString("author"));
        fileData.setComment(rs.getString("file_comment"));
        fileData.setModifiedAt(rs.getTimestamp("modified_at"));
        fileData.setVersion(rs.getString("id"));
        fileData.setDeleted(rs.getBoolean("deleted"));
        return fileData;
    }

    protected void safeClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    protected void safeClose(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
            } catch (Exception e) {
                log.warn("Failed to commit", e);
            }
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    private String makePathPattern(String path) {
        return path.replace("$", "$$").replace("%", "$%") + "%";
    }

    protected void safeClose(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (Exception e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    private void invokeListener() {
        monitor.fireOnChange();
    }

    @Override
    public void close() throws IOException {
        if (monitor != null) {
            monitor.release();
            monitor = null;
        }
    }

    private FileData insertFile(FileData data, InputStream stream) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.insertFile);
            statement.setString(1, data.getName());
            statement.setString(2, data.getAuthor());
            statement.setString(3, data.getComment());
            if (stream != null) {
                statement.setBinaryStream(4, stream);
            } else {
                // Workaround for PostreSQL
                statement.setBinaryStream(4, null, 0);
            }

            statement.executeUpdate();
            
            data.setVersion(null);
            invokeListener();
            return data;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public void initialize() {
        JDBCDriverRegister.registerDrivers();
        Exception actualException = null;
        try {
            Connection connection = getConnection();
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                String databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
                int majorVersion = metaData.getDatabaseMajorVersion();
                int minorVersion = metaData.getDatabaseMinorVersion();

                log.info("Driver name      : {}", metaData.getDriverName());
                log.info("Driver version   : {}", metaData.getDriverVersion());
                log.info("Database name    : {}", metaData.getDatabaseProductName());
                log.info("Database version : {}", metaData.getDatabaseProductVersion());
                log.info("Database code    : {}-v{}.{}", databaseCode, majorVersion, minorVersion);
                settings = new Settings(databaseCode, majorVersion, minorVersion);
                initializeDatabase(connection);
                monitor = new ChangesMonitor(new DBRepositoryRevisionGetter(), settings.timerPeriod);
            } catch (Exception e) {
                actualException = e;
            } finally {
                try {
                    if (!connection.getAutoCommit()) {
                        connection.commit();
                    }
                } catch (Exception e) {
                    if (actualException == null) {
                        actualException = e;
                    }
                }
                try {
                    connection.close();
                } catch (Exception e) {
                    if (actualException == null) {
                        actualException = e;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize a repository", e);
        }
        if (actualException != null) {
            throw new IllegalStateException("Failed to initialize a repository", actualException);
        }
    }

    private void initializeDatabase(Connection connection) throws SQLException {
        Object revision = checkRepository(connection);
        if (!(revision instanceof Throwable)) {
            log.info("SQL result: {}. The repository is already initialized.", revision);
            return;
        }
        log.info("SQL error: {}", ((Throwable)revision).getMessage());
        log.info("Initializing  the repository in the DB...");
        Statement statement = connection.createStatement();
        try {
            for (String query : settings.initStatements) {
                if (StringUtils.isNotBlank(query)) {
                    statement.execute(query);
                }
            }
            log.info("The repository has been initialized.");
        } finally {
            safeClose(statement);
        }
    }

    private class DBRepositoryRevisionGetter implements RevisionGetter {

        @Override
        public Object getRevision() {
            Connection connection = null;
            try {
                connection = getConnection();
                Object revision = checkRepository(connection);
                if (revision instanceof Throwable) {
                    log.warn("Cannot to check revision of the repository", revision);
                    return null;
                }
                return revision;
            } catch (Exception e) {
                log.warn("Cannot to check revision of the repository", e);
                return null;
            } finally {
                safeClose(connection);
            }
        }
    }

    /**
     * Return a hash of the repository or an exception.
     */
    private Object checkRepository(final Connection connection) {
        String changeSet = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement(settings.selectLastChange);
            rs = statement.executeQuery();

            if (rs.next()) {
                changeSet = rs.getString(1);
            }
        } catch (Exception e) {
            return e;
        } finally {
            safeClose(rs);
            safeClose(statement);
        }
        return changeSet;
    }
}
