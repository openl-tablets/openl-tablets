package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.RootFolderExtractor;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.util.FileUtils;

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
        try (ZipInputStream zipInputStream = getZipInputStream()) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String name = zipEntry.getName();
                FileUtils.getValidPath(name);
                String fileName = folderExtractor.extractFromRootFolder(name);
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
        }
    }

    private ZipInputStream getZipInputStream() throws IOException {
        return new ZipInputStream(uploadedFile.getInput(), charset);
    }

    private RootFolderExtractor createFolderExtractor(PathFilter zipFilter) throws IOException {
        try (ZipInputStream zipInputStream = getZipInputStream()) {
            Set<String> names = new TreeSet<>();
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String name = zipEntry.getName();
                FileUtils.getValidPath(name);
                names.add(name);
            }
            zipInputStream.close();

            return new RootFolderExtractor(names, zipFilter);
        }
    }

}
