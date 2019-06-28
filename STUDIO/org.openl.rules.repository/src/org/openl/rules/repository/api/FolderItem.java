package org.openl.rules.repository.api;

public class FolderItem {

    private final FileData data;
    private final Iterable<FileItem> files;

    public FolderItem(FileData data, Iterable<FileItem> files) {
        this.data = data;
        this.files = files;
    }

    public FileData getData() {
        return data;
    }

    public Iterable<FileItem> getFiles() {
        return files;
    }
}
