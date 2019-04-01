package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FilePathsCollector extends DefaultZipEntryCommand {
    private final List<String> filePaths = new ArrayList<>();

    @Override
    public boolean execute(String filePath, InputStream inputStream) {
        filePaths.add(filePath);
        return true;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }
}
