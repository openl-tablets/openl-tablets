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

import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.rules.repository.common.RevisionGetter;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.db.JDBCDriverRegister;
import org.openl.util.db.SqlDBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class DBRepository implements Repository, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(DBRepository.class);

    private String id;
    private String name;
    private Settings settings;
    private ChangesMonitor monitor;
    private int listenerTimerPeriod = 10;

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.selectAllMetaInfo);
            statement.setString(1, makePathPattern(path));
            rs = statement.executeQuery();

            List<FileData> filesData = new ArrayList<>();
            while (rs.next()) {
                FileData fileData = createFileData(rs);
                filesData.add(fileData);
            }

            rs.close();
            statement.close();

            return filesData;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
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
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        FileData result = insertFile(data, stream);
        invokeListener();
        return result;
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        List<FileData> result = new ArrayList<>();
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            for (FileItem fileItem : fileItems) {
                FileData data = fileItem.getData();
                PreparedStatement statement = null;
                try {
                    statement = createInsertFileStatement(connection, data, fileItem.getStream());
                    statement.executeUpdate();
                } finally {
                    SqlDBUtils.safeClose(statement);
                }
                data.setVersion(null);
                result.add(data);
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(connection);
        }
        invokeListener();
        return result;
    }

    @Override
    public boolean delete(FileData path) throws IOException {
        FileData data = getLatestVersionFileData(path.getName());
        if (data != null) {
            insertFile(data, null);
            invokeListener();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        if (data.isEmpty()) {
            return false;
        }
        boolean deleted = false;
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            for (FileData f : data) {
                FileData lastVersion = getLatestVersionFileData(connection, f.getName());
                if (lastVersion != null) {
                    try (PreparedStatement statement = createInsertFileStatement(connection, lastVersion, null)) {
                        statement.executeUpdate();
                        deleted = true;
                    } catch (SQLException e) {
                        throw new IOException(e);
                    }
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IOException(e);
        }
        if (deleted) {
            invokeListener();
        }
        return deleted;
    }

    private FileData copy(String srcName, FileData destData) throws IOException {
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
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
        }
        invokeListener();
        return destData;
    }

    @Override
    public void setListener(final Listener callback) {
        if (monitor != null) {
            monitor.setListener(callback);
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.selectAllHistoryMetaInfo);
            statement.setString(1, name);
            rs = statement.executeQuery();

            List<FileData> filesData = new ArrayList<>();
            while (rs.next()) {
                FileData fileData = createFileData(rs);
                filesData.add(fileData);
            }

            rs.close();
            statement.close();

            return filesData;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
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
            statement.setLong(1, Long.parseLong(version));
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
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
        }
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        String dataName = data.getName();
        String version = data.getVersion();

        Connection connection = null;
        PreparedStatement statement = null;
        if (version == null) {
            try {
                connection = getConnection();
                statement = connection.prepareStatement(settings.deleteAllHistory);
                statement.setString(1, dataName);
                int rows = statement.executeUpdate();

                if (rows > 0) {
                    invokeListener();
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            } finally {
                SqlDBUtils.safeClose(statement);
                SqlDBUtils.safeClose(connection);
            }
        } else {
            try {
                connection = getConnection();
                statement = connection.prepareStatement(settings.deleteVersion);
                statement.setLong(1, Long.parseLong(version));
                statement.setString(2, dataName);
                int rows = statement.executeUpdate();

                if (rows > 0) {
                    invokeListener();
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            } finally {
                SqlDBUtils.safeClose(statement);
                SqlDBUtils.safeClose(connection);
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

            statement.setLong(4, Long.parseLong(version));
            statement.setString(5, srcName);
            statement.executeUpdate();

            invokeListener();
            return destData;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
        }
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).build();
    }

    protected abstract Connection getConnection() throws SQLException;

    private FileData getLatestVersionFileData(String name) throws IOException {
        Connection connection = null;
        try {
            connection = getConnection();
            return getLatestVersionFileData(connection, name);
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(connection);
        }
    }

    private FileData getLatestVersionFileData(Connection connection, String name) throws IOException {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.prepareStatement(settings.readActualFileMetaInfo);
            statement.setString(1, name);
            rs = statement.executeQuery();

            FileData fileData = null;
            if (rs.next()) {
                fileData = createFileData(rs);
            }
            return fileData;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(statement);
        }
    }

    private FileData getHistoryVersionFileData(String name, String version) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.readHistoricFileMetaInfo);
            statement.setLong(1, Long.parseLong(version));
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
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
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

    private String makePathPattern(String path) {
        return path.replace("$", "$$").replace("%", "$%") + "%";
    }

    private void invokeListener() {
        monitor.fireOnChange();
    }

    @Override
    public void close() {
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
            statement = createInsertFileStatement(connection, data, stream);
            statement.executeUpdate();
            data.setVersion(null);
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
        }
        return data;
    }

    private PreparedStatement createInsertFileStatement(Connection connection,
            FileData data,
            InputStream stream) throws SQLException {

        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(settings.insertFile);
            statement.setString(1, data.getName());
            statement.setString(2, data.getAuthor());
            statement.setString(3, data.getComment());
            if (stream != null) {
                statement.setBinaryStream(4, stream);
            } else {
                // Workaround for PostgreSQL
                statement.setBinaryStream(4, null, 0);
            }
            return statement;
        } catch (Exception e) {
            // If exception is thrown, we must close statement in this method and rethrow exception.
            // If no exception, statement will be closed later.
            if (statement != null) {
                SqlDBUtils.safeClose(statement);
            }
            throw e;
        }
    }

    public void initialize() {
        try {
            JDBCDriverRegister.registerDrivers();
            loadDBSettings();
            initializeDatabase();
            monitor = new ChangesMonitor(new DBRepositoryRevisionGetter(), listenerTimerPeriod);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize a repository.", e);
        }
    }

    private void loadDBSettings() throws IOException, SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
            int majorVersion = metaData.getDatabaseMajorVersion();
            int minorVersion = metaData.getDatabaseMinorVersion();

            LOG.info("Driver name      : {}", metaData.getDriverName());
            LOG.info("Driver version   : {}", metaData.getDriverVersion());
            LOG.info("Database name    : {}", metaData.getDatabaseProductName());
            LOG.info("Database version : {}", metaData.getDatabaseProductVersion());
            LOG.info("Database code    : {}-v{}.{}", databaseCode, majorVersion, minorVersion);
            settings = new Settings(databaseCode, majorVersion, minorVersion);
        } finally {
            SqlDBUtils.safeClose(connection);
        }
    }

    @SuppressWarnings("squid:S2095") // Statement is closed by safeClose(...) method in finally block
    private void initializeDatabase() throws SQLException {
        Object revision = checkRepository();
        if (!(revision instanceof Throwable)) {
            LOG.info("SQL result: {}. The repository is already initialized.", revision);
            return;
        }
        LOG.info("SQL error: {}", ((Throwable) revision).getMessage());
        LOG.info("Initializing the repository in the DB...");
        Connection connection = null;
        Statement statement = null;
        Boolean autoCommit = null;
        try {
            connection = getConnection();
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            statement = connection.createStatement();

            for (String query : settings.initStatements) {
                if (StringUtils.isNotBlank(query)) {
                    statement.execute(query);
                }
            }
            connection.commit();
            LOG.info("The repository is initialized.");
        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
            }
            throw e;
        } finally {
            if (autoCommit != null) {
                connection.setAutoCommit(autoCommit);
            }
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
        }
    }

    private class DBRepositoryRevisionGetter implements RevisionGetter {

        @Override
        public Object getRevision() {
            Object revision = checkRepository();
            if (revision instanceof Throwable) {
                LOG.warn("Cannot check revision of the repository.", (Throwable) revision);
                return null;
            }
            return revision;
        }
    }

    /**
     * Return a hash of the repository or an exception.
     */
    private Object checkRepository() {
        String changeSet = null;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(settings.selectLastChange);
            rs = statement.executeQuery();

            if (rs.next()) {
                changeSet = rs.getString(1);
            }
        } catch (Exception e) {
            return e;
        } finally {
            SqlDBUtils.safeClose(rs);
            SqlDBUtils.safeClose(statement);
            SqlDBUtils.safeClose(connection);
        }
        return changeSet;
    }
}
