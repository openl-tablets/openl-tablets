package org.openl.rules.repository.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.Date;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DBRepository implements Repository {
    private final Logger log = LoggerFactory.getLogger(DBRepository.class);

    private Listener listener;
    private Timer timer;

    @Override
    public List<FileData> list(String path) {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement(DatabaseQueries.SELECT_ALL_METAINFO);
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
            throw new IllegalStateException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
        }
    }

    @Override
    public FileItem read(String name) {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement(DatabaseQueries.READ_ACTUAL_FILE);
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
            throw new IllegalStateException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
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
    public boolean delete(String path) {
        FileData data = getLatestVersionFileData(path);
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
    public FileData copy(String srcPath, String destPath) {
        // TODO: implement
        return null;
    }

    @Override
    public FileData rename(String path, String destination) {
        // TODO: implement
        return null;
    }

    @Override
    public void setListener(final Listener callback) {
        this.listener = callback;

        if (callback == null) {
            timer.cancel();
            timer = null;
        } else {
            if (timer == null) {
                timer = new Timer(true);
            }

            timer.schedule(new TimerTask() {
                private Long maxId = null;

                @Override
                public void run() {
                    PreparedStatement statement = null;
                    ResultSet rs = null;
                    try {
                        statement = getConnection().prepareStatement(DatabaseQueries.SELECT_MAX_ID);
                        rs = statement.executeQuery();

                        if (rs.next()) {
                            long newMaxId = rs.getLong("max_id");
                            if (maxId == null) {
                                maxId = newMaxId;
                            } else if (newMaxId > maxId) {
                                maxId = newMaxId;
                                callback.onChange();
                            }
                        }

                        rs.close();
                        statement.close();
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    } finally {
                        safeClose(rs);
                        safeClose(statement);
                    }
                }
            }, 1000, 1000);
        }
    }

    @Override
    public List<FileData> listHistory(String name) {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement(DatabaseQueries.SELECT_ALL_HISTORY_METAINFO_FOR_FILE);
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
            throw new IllegalStateException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
        }
    }

    @Override
    public FileItem readHistory(String name, String version) {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement(DatabaseQueries.READ_HISTORIC_FILE);
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
            throw new IllegalStateException(e);
        } finally {
            safeClose(rs);
            safeClose(statement);
        }
    }

    @Override
    public boolean deleteHistory(String name, String version) {
        if (version == null) {
            PreparedStatement statement = null;
            try {
                statement = getConnection().prepareStatement(DatabaseQueries.DELETE_ALL_HISTORY);
                statement.setString(1, name);
                int rows = statement.executeUpdate();

                if (rows > 0) {
                    invokeListener();
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            } finally {
                safeClose(statement);
            }
        } else {
            PreparedStatement statement = null;
            try {
                statement = getConnection().prepareStatement(DatabaseQueries.DELETE_VERSION);
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
                throw new IllegalStateException(e);
            } finally {
                safeClose(statement);
            }
        }
    }

    @Override
    public FileData copyHistory(String srcName, String destName, String version) {
        // TODO: implement
        return null;
    }

    protected abstract Connection getConnection();

    private FileData getLatestVersionFileData(String name) {
        FileItem existingItem = read(name);// TODO: Use separate query instead of querying FileItem
        if (existingItem != null) {
            IOUtils.closeQuietly(existingItem.getStream());
            return existingItem.getData();
        }
        return null;
    }

    private FileItem createFileItem(ResultSet rs) throws SQLException {
        FileData fileData = createFileData(rs);
        InputStream data = rs.getBinaryStream("file_data");
        if (data == null) {
            return new FileItem(fileData, null);
        }

        // ResultSet will be closed, so InputStream can be closed too, that's why copy it to byte array before.
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

    public void close() throws IOException {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private FileData insertFile(FileData data, InputStream stream) throws IOException {
        PreparedStatement statement = null;
        try {
            statement = getConnection().prepareStatement(DatabaseQueries.INSERT_FILE);

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
        }
    }
}
