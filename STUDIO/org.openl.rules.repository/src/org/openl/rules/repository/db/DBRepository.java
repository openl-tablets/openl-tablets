package org.openl.rules.repository.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DBRepository implements Repository, Closeable, RRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(DBRepository.class);

    private Listener listener;
    private Timer timer;
    private int timerPeriod = 10000;

    private Map<String, String> queries;

    @Override
    public List<FileData> list(String path) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(queries.get(DatabaseQueries.SELECT_ALL_METAINFO));
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
            statement = connection.prepareStatement(queries.get(DatabaseQueries.READ_ACTUAL_FILE));
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
        if (!data.isDeleted()) {
            FileData existing = getLatestVersionFileData(data.getName());

            if (existing != null && existing.isDeleted()) {
                // This is undelete operation
                deleteHistory(data.getName(), existing.getVersion());
                invokeListener();
                return getLatestVersionFileData(data.getName());
            }
        }

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
            String newVersion = UUID.randomUUID().toString();

            connection = getConnection();
            statement = connection.prepareStatement(queries.get(DatabaseQueries.COPY_FILE));
            statement.setString(1, destData.getName());
            statement.setTimestamp(2, new Timestamp(new Date().getTime()));
            statement.setString(3, newVersion);
            statement.setString(4, srcName);
            statement.executeUpdate();

            FileData copy = getHistoryVersionFileData(destData.getName(), newVersion);
            invokeListener();
            return copy;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public FileData rename(String path, FileData destData) throws IOException {
        // TODO: implement
        return null;
    }

    @Override
    public void setListener(final Listener callback) {
        this.listener = callback;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (callback != null) {
            timer = new Timer(true);

            timer.schedule(new TimerTask() {
                private Long maxId = null;
                private Long countId = null;

                @Override
                public void run() {
                    Connection connection = null;
                    PreparedStatement statement = null;
                    ResultSet rs = null;
                    try {
                        connection = getConnection();
                        statement = connection.prepareStatement(queries.get(DatabaseQueries.SELECT_MAX_ID));
                        rs = statement.executeQuery();

                        if (rs.next()) {
                            long newMaxId = rs.getLong("max_id");
                            long newCountId = rs.getLong("count_id");

                            if (maxId == null) {
                                maxId = newMaxId;
                                countId = newCountId;
                            } else if (newMaxId != maxId || newCountId != countId) {
                                maxId = newMaxId;
                                countId = newCountId;
                                callback.onChange();
                            }
                        }

                        rs.close();
                        statement.close();
                    } catch (SQLException e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        safeClose(rs);
                        safeClose(statement);
                        safeClose(connection);
                    }
                }
            }, 1000, timerPeriod);
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(queries.get(DatabaseQueries.SELECT_ALL_HISTORY_METAINFO));
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
            statement = connection.prepareStatement(queries.get(DatabaseQueries.READ_HISTORIC_FILE));
            statement.setString(1, name);
            statement.setString(2, version);
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
                statement = connection.prepareStatement(queries.get(DatabaseQueries.DELETE_ALL_HISTORY));
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
                statement = connection.prepareStatement(queries.get(DatabaseQueries.DELETE_VERSION));
                statement.setString(1, name);
                statement.setString(2, version);
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
            String newVersion = UUID.randomUUID().toString();

            connection = getConnection();
            statement = connection.prepareStatement(queries.get(DatabaseQueries.COPY_HISTORY));
            statement.setString(1, destData.getName());
            statement.setTimestamp(2, new Timestamp(new Date().getTime()));
            statement.setString(3, newVersion);
            statement.setString(4, srcName);
            statement.setString(5, version);
            statement.executeUpdate();

            FileData copy = getHistoryVersionFileData(destData.getName(), newVersion);
            invokeListener();
            return copy;
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
            statement = connection.prepareStatement(queries.get(DatabaseQueries.READ_ACTUAL_FILE_METAINFO));
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
            statement = connection.prepareStatement(queries.get(DatabaseQueries.READ_HISTORIC_FILE_METAINFO));
            statement.setString(1, name);
            statement.setString(2, version);
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
            return new FileItem(fileData, null);
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
        fileData.setModifiedAt(rs.getDate("modified_at"));
        fileData.setVersion(rs.getString("version"));
        fileData.setDeleted(rs.getBoolean("deleted"));
        return fileData;
    }

    protected void safeClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    protected void safeClose(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
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
            } catch (SQLException e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    private void invokeListener() {
        if (listener != null) {
            listener.onChange();
        }
    }

    @Override
    public void close() throws IOException {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private FileData insertFile(FileData data, InputStream stream) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(queries.get(DatabaseQueries.INSERT_FILE));

            String version = UUID.randomUUID().toString();

            statement.setString(1, data.getName());
            statement.setLong(2, stream == null ? 0 : data.getSize());
            statement.setString(3, data.getAuthor());
            statement.setString(4, data.getComment());
            statement.setTimestamp(5, new Timestamp(new Date().getTime()));
            statement.setString(6, version);
            if (stream != null) {
                statement.setBinaryStream(7, stream);
            } else {
                statement.setBinaryStream(7, null, 0);
            }

            statement.executeUpdate();

            data.setVersion(version);
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
        registerDrivers();
        Throwable actualException = null;
        try {
            Connection connection = getConnection();
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                String databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
                log.info("Database product name is [{}]", databaseCode);
                queries = new HashMap<String, String>();
                fillQueries(queries, "/openl-db-repository.properties");
                fillQueries(queries, "/openl-db-repository-" + databaseCode + ".properties");
                fillQueries(queries, "/openl-db-repository-ext.properties");

                String timerPeriod = queries.get(DatabaseQueries.SETTING_TIMER_PERIOD);
                if (timerPeriod != null) {
                    try {
                        this.timerPeriod = Integer.parseInt(timerPeriod);
                    } catch (Exception e) {
                        log.warn("Cannot parse value from {} = {}! Default value is used.", DatabaseQueries.SETTING_TIMER_PERIOD, timerPeriod, e);
                    }
                }
                initializeDatabase(connection, databaseCode);
            } catch (Throwable e) {
                actualException = e;
            } finally {
                try {
                    connection.close();
                } catch (Throwable e) {
                    if (actualException == null) {
                        actualException = e;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize repository", e);
        }
        if (actualException != null) {
            throw new IllegalStateException("Failed to initialize repository", actualException);
        }
    }

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

    private void initializeDatabase(Connection connection, String databaseCode) throws SQLException {
        if (!tableExists(connection, databaseCode)) {
            List<String> queryKeys = new ArrayList<String>();
            for (String key : queries.keySet()) {
                if (key.startsWith(DatabaseQueries.INIT_PREFIX)) {
                    queryKeys.add(key);
                }
            }

            Collections.sort(queryKeys);

            for (String key : queryKeys) {
                String query = queries.get(key);
                if (!StringUtils.isBlank(query)) {
                    Statement statement = null;
                    try {
                        statement = connection.createStatement();
                        statement.execute(query);
                    } finally {
                        safeClose(statement);
                    }

                }
            }
        }
    }

    private boolean tableExists(Connection connection, String databaseCode) throws SQLException {
        ResultSet rs = null;
        String tableName = queries.get(DatabaseQueries.REPOSITORY_NAME);
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String repoTable = metaData.storesUpperCaseIdentifiers() ? tableName.toUpperCase() : tableName;
            if ("oracle".equals(databaseCode)) {
                rs = metaData.getTables(null, metaData.getUserName(), repoTable, new String[] { "TABLE" });
            } else {
                rs = metaData.getTables(null, null, repoTable, new String[] { "TABLE" });
            }
            return rs.next();
        } finally {
            safeClose(rs);
        }
    }

    private void fillQueries(Map<String, String> queries, String propertiesFileName) throws IOException {
        URL resource = getClass().getResource(propertiesFileName);
        if (resource == null) {
            log.info("File [{}] not found.", propertiesFileName);
            return;
        }
        log.info("Load configuration from [{}].", resource);
        InputStream is = resource.openStream();
        try {
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                queries.put(key, properties.getProperty(key));
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
