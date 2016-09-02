package org.openl.rules.repository.api;

import java.io.InputStream;
import java.util.List;

/**
 * @author Yury Molchan
 */
public interface Repository {

    List<FileData> list(String path);

    FileItem read(String name);

    FileData save(FileData data, InputStream stream);

    boolean delete(String path);

    FileData copy(String srcPath, String destPath);

    FileData rename(String path, String destination);

    void setListener(Listener callback);

    List<FileData> listHistory(String name);

    FileItem readHistory(String name, String version);

    boolean deleteHistory(String name, String version);

    FileData copyHistory(String srcName, String destName, String version);

}
