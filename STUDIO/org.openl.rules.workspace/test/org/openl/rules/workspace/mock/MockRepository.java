package org.openl.rules.workspace.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;

public class MockRepository implements Repository {
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
    public FileData check(String name) throws IOException {
        FileItem fileItem = read(name);
        if (fileItem == null) {
            return null;
        }
        IOUtils.closeQuietly(fileItem.getStream());
        return fileItem.getData();
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
    public FileData copy(String srcPath, FileData destData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData rename(String path, FileData destData) {
        FileData fileData = fileDataMap.remove(path);
        FileItem fileItem = fileItemMap.remove(path);

        fileData.setName(destData.getName());
        fileItem.getData().setName(destData.getName());
        fileDataMap.put(destData.getName(), fileData);
        fileItemMap.put(destData.getName(), fileItem);

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
    public FileData checkHistory(String name, String version) throws IOException {
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
    public FileData copyHistory(String srcName, FileData destData, String version) {
        throw new UnsupportedOperationException();
    }
}
