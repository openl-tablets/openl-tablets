package org.openl.rules.repository.api;

public class FolderItem {

    private final FileData data;
    private final Iterable<FileChange> files;

    public FolderItem(FileData data, Iterable<FileChange> files) {
        this.data = data;
        this.files = files;
    }

    public FileData getData() {
        return data;
    }

    public Iterable<FileChange> getFiles() {
        return files;
    }
}
