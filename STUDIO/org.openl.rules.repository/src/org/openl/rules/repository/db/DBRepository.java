package org.openl.rules.repository.db;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.rules.repository.common.RevisionGetter;
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

            List<FileData> fileDatas = new ArrayList<>();
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
                    safeClose(statement);
                }
                data.setVersion(null);
                result.add(data);
            }
            connection.commit();
            invokeListener();
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(connection);
        }
        return result;
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
            statement = connection.prepareStatement(settings.selectAllHistoryMetainfo);
            statement.setString(1, name);
            rs = statement.executeQuery();

            List<FileData> fileDatas = new ArrayList<>();
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
    public boolean deleteHistory(FileData data) {
        String name = data.getName();
        String version = data.getVersion();

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

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).build();
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
            invokeListener();
            return data;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    private PreparedStatement createInsertFileStatement(Connection connection,
            FileData data,
            InputStream stream) throws SQLException {

        PreparedStatement statement = connection.prepareStatement(settings.insertFile);
        statement.setString(1, data.getName());
        statement.setString(2, data.getAuthor());
        statement.setString(3, data.getComment());
        if (stream != null) {
            statement.setBinaryStream(4, stream);
        } else {
            // Workaround for PostreSQL
            statement.setBinaryStream(4, null, 0);
        }
        return statement;
    }

    @Override
    public void initialize() {
        try {
            JDBCDriverRegister.registerDrivers();
            loadDBsettings();
            initializeDatabase();
            monitor = new ChangesMonitor(new DBRepositoryRevisionGetter(), settings.timerPeriod);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize a repository", e);
        }
    }

    private void loadDBsettings() throws IOException, SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
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
        } finally {
            safeClose(connection);
        }
    }

    @SuppressWarnings("squid:S2095") // Statement is closed by safeClose(...) method in finally block
    private void initializeDatabase() throws SQLException {
        Object revision = checkRepository();
        if (!(revision instanceof Throwable)) {
            log.info("SQL result: {}. The repository is already initialized.", revision);
            return;
        }
        log.info("SQL error: {}", ((Throwable) revision).getMessage());
        log.info("Initializing  the repository in the DB...");
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
            log.info("The repository has been initialized.");
        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
            }
            throw e;
        } finally {
            if (autoCommit != null) {
                connection.setAutoCommit(autoCommit);
            }
            safeClose(statement);
            safeClose(connection);
        }
    }

    private class DBRepositoryRevisionGetter implements RevisionGetter {

        @Override
        public Object getRevision() {
            Object revision = checkRepository();
            if (revision instanceof Throwable) {
                log.warn("Cannot to check revision of the repository", revision);
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
            safeClose(rs);
            safeClose(statement);
            safeClose(connection);
        }
        return changeSet;
    }
}
