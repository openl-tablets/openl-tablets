package org.openl.rules.workspace.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.openl.rules.project.impl.local.FolderRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;

public class MockRepository implements Repository, FolderRepository {
    private Map<String, FileData> fileDataMap = new HashMap<String, FileData>();
    private Map<String, FileItem> fileItemMap = new HashMap<String, FileItem>();
    @Override
    public List<FileData> list(String path) throws IOException {
        List<FileData> result = new ArrayList<FileData>();
        for (Map.Entry<String, FileData> entry : fileDataMap.entrySet()) {
            if (entry.getKey().startsWith(path)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    @Override
    public FileItem read(String name) {
        return fileItemMap.get(name);
    }

    @Override
    public FileData save(FileData data, InputStream stream) {
        fileDataMap.put(data.getName(), data);
        fileItemMap.put(data.getName(), new FileItem(data, stream));
        return data;
    }

    @Override
    public boolean delete(String path) {
        return fileDataMap.remove(path) != null || fileItemMap.remove(path) != null;
    }

    @Override
    public FileData copy(String srcPath, String destPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData rename(String path, String destination) {
        FileData fileData = fileDataMap.remove(path);
        FileItem fileItem = fileItemMap.remove(path);

        fileData.setName(destination);
        fileItem.getData().setName(destination);
        fileDataMap.put(destination, fileData);
        fileItemMap.put(destination, fileItem);

        return fileData;
    }

    @Override
    public void setListener(Listener callback) {

    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileItem readHistory(String name, String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteHistory(String name, String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData copyHistory(String srcName, String destName, String version) {
        throw new UnsupportedOperationException();
    }
}
