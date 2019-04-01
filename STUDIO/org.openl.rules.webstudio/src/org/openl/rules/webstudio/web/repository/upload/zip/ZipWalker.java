package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.RootFolderExtractor;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.util.IOUtils;

public class ZipWalker {
    private final ProjectFile uploadedFile;
    private final Charset charset;
    private final RootFolderExtractor folderExtractor;

    public ZipWalker(ProjectFile uploadedFile, PathFilter zipFilter, Charset charset) throws IOException {
        this.uploadedFile = uploadedFile;
        this.charset = charset;
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

    private ZipInputStream getZipInputStream() {
        return new ZipInputStream(uploadedFile.getInput(), charset);
    }

    private RootFolderExtractor createFolderExtractor(PathFilter zipFilter) throws IOException {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = getZipInputStream();
            Set<String> names = new TreeSet<>();
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
