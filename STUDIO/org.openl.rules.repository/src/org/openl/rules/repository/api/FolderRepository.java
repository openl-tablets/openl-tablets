package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.List;

public interface FolderRepository extends Repository {
    List<FileData> listFolders(String path) throws IOException;

    FileData save(FileData folderData, List<FileChange> files) throws IOException;
}
