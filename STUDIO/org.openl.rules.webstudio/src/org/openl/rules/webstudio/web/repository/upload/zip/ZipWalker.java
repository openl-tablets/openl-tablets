package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.openl.rules.webstudio.web.repository.upload.RootFolderExtractor;
import org.openl.rules.workspace.filter.PathFilter;
import org.richfaces.model.UploadedFile;

public class ZipWalker {
    private final UploadedFile uploadedFile;
    private final File zipFile;
    private final RootFolderExtractor folderExtractor;

    public ZipWalker(UploadedFile uploadedFile, PathFilter zipFilter) throws IOException {
        this.uploadedFile = uploadedFile;
        this.zipFile = null;
        this.folderExtractor = createFolderExtractor(zipFilter);
    }

    public ZipWalker(File zipFile, PathFilter zipFilter) throws IOException {
        this.uploadedFile = null;
        this.zipFile = zipFile;
        folderExtractor = createFolderExtractor(zipFilter);
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
        return uploadedFile != null ? new ZipInputStream(uploadedFile.getInputStream()) : new ZipInputStream(new FileInputStream(zipFile));
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
