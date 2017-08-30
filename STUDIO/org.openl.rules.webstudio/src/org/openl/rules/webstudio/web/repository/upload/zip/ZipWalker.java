package org.openl.rules.webstudio.web.repository.upload.zip;

import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.RootFolderExtractor;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipWalker {
    private final ProjectFile uploadedFile;
    private final File zipFile;
    private final RootFolderExtractor folderExtractor;

    public ZipWalker(ProjectFile uploadedFile, PathFilter zipFilter) throws IOException {
        this.uploadedFile = uploadedFile;
        this.zipFile = null;
        this.folderExtractor = createFolderExtractor(zipFilter);
    }

    public void iterateEntries(ZipEntryCommand command) throws IOException {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = getZipInputStream();
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String fileName = folderExtractor.extractFromRootFolder(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!command.execute(fileName)) {
                        break;
                    }
                } else {
                    if (!command.execute(fileName, zipInputStream)) {
                        break;
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }

    }

    private ZipInputStream getZipInputStream() throws FileNotFoundException {
        return uploadedFile != null ? new ZipInputStream(uploadedFile.getInput()) : new ZipInputStream(new FileInputStream(zipFile));
    }

    private RootFolderExtractor createFolderExtractor(PathFilter zipFilter) throws IOException {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = getZipInputStream();
            Set<String> names = new TreeSet<String>();
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                names.add(zipEntry.getName());
            }
            zipInputStream.close();

            return new RootFolderExtractor(names, zipFilter);
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
    }

}
