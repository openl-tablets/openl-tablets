package org.openl.rules.repository.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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

    @Override
    public List<FileData> list(String path) {
        String sql = "select r1.id, r1.file_name, r1.file_size, r1.author, r1.file_comment, r1.modified_at, r1.version, case when r1.file_data is null then 1 else 0 end as deleted\n"
                + "from openl_repository r1\n"
                + "inner join (\n"
                + "\tselect max(id) as id\n"
                + "\tfrom openl_repository\n"
                + "\twhere file_name like ? escape '$'\n"
                + "\tgroup by file_name\n"
                + ") r2\n"
                + "on r1.id = r2.id\n"
                + "order by r1.file_name";
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement(sql);
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
        String sql = "select r1.id, r1.file_name, r1.file_size, r1.author, r1.file_comment, r1.modified_at, r1.version, case when r1.file_data is null then 1 else 0 end as deleted, r1.file_data\n"
                + "from openl_repository r1\n"
                + "inner join (\n"
                + "\tselect max(id) as id\n"
                + "\tfrom openl_repository\n"
                + "\twhere file_name = ?\n"
                + ") r2\n"
                + "on r1.id = r2.id";
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement(sql);
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
    public FileData save(FileData data, InputStream stream) {
        if (!data.isDeleted()) {
            FileData existing = getLatestVersionFileData(data.getName());

            if (existing != null && existing.isDeleted()) {
                // This is undelete operation
                deleteHistory(data.getName(), existing.getVersion());
                invokeListener();
                return getLatestVersionFileData(data.getName());
            }
        }

        String sql = "insert into openl_repository(file_name, file_size, author, file_comment, modified_at, version, file_data)\n"
                + "values(?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = null;
        try {
            statement = getConnection().prepareStatement(sql);

            String version = UUID.randomUUID().toString();

            statement.setString(1, data.getName());
            statement.setLong(2, data.getSize());
            statement.setString(3, data.getAuthor());
            statement.setString(4, data.getComment());
            statement.setTimestamp(5, new Timestamp(new Date().getTime()));
            statement.setString(6, version);
            statement.setBinaryStream(7, stream);

            statement.executeUpdate();

            data.setVersion(version);
            invokeListener();
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            safeClose(statement);
        }
    }

    @Override
    public boolean delete(String path) {
        FileData data = getLatestVersionFileData(path);
        if (data != null) {
            String sql = "insert into openl_repository(file_name, file_size, author, file_comment, modified_at, version, file_data)\n"
                    + "values(?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = null;
            try {
                statement = getConnection().prepareStatement(sql);

                statement.setString(1, data.getName());
                statement.setLong(2, 0);
                statement.setString(3, data.getAuthor());
                statement.setString(4, data.getComment());
                statement.setTimestamp(5, new Timestamp(new Date().getTime()));
                statement.setString(6, UUID.randomUUID().toString());
                statement.setBinaryStream(7, null, 0);

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
    public void setListener(Listener callback) {
        this.listener = callback;
    }

    @Override
    public List<FileData> listHistory(String name) {
        String sql = "select id, file_name, file_size, author, file_comment, modified_at, version, case when file_data is null then 1 else 0 end as deleted\n"
                + "from openl_repository\n"
                + "where file_name = ?\n"
                + "order by file_name";
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement(sql);
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
        String sql = "select id, file_name, file_size, author, file_comment, modified_at, version, case when file_data is null then 1 else 0 end as deleted, file_data\n"
                + "from openl_repository\n"
                + "where file_name = ? and version = ?\n"
                + "order by file_name";
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().prepareStatement(sql);
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
            String sql = "delete from openl_repository where file_name = ?";
            PreparedStatement statement = null;
            try {
                statement = getConnection().prepareStatement(sql);
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
            String sql = "delete from openl_repository where file_name = ? and version = ?";
            PreparedStatement statement = null;
            try {
                statement = getConnection().prepareStatement(sql);
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

    private void safeClose(ResultSet rs) {
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

    private void safeClose(Statement st) {
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
}
