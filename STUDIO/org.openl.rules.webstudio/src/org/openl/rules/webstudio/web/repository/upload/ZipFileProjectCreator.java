package org.openl.rules.webstudio.web.repository.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import lombok.extern.slf4j.Slf4j;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipFromFile;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

@Slf4j
public class ZipFileProjectCreator extends AProjectCreator {

    private static final long MAX_FILE_SIZE = 1000L * 1024 * 1024;

    private final ZipFile zipFile;
    private final PathFilter zipFilter;
    private final File uploadedFile;
    private final Charset charset;
    private final String comment;
    private final Repository repository;

    public ZipFileProjectCreator(String repositoryId,
                                 String uploadedFileName,
                                 InputStream uploadedFileStream,
                                 String projectName,
                                 String projectFolder,
                                 UserWorkspace userWorkspace,
                                 String comment,
                                 PathFilter zipFilter,
                                 ZipCharsetDetector zipCharsetDetector,
                                 Map<String, String> tags) throws IOException {
        this(userWorkspace.getDesignTimeRepository().getRepository(repositoryId),
                uploadedFileName,
                uploadedFileStream,
                projectName,
                projectFolder,
                userWorkspace,
                comment,
                zipFilter,
                zipCharsetDetector,
                tags);
    }

    public ZipFileProjectCreator(Repository repository,
                                 String uploadedFileName,
                                 InputStream uploadedFileStream,
                                 String projectName,
                                 String projectFolder,
                                 UserWorkspace userWorkspace,
                                 String comment,
                                 PathFilter zipFilter,
                                 ZipCharsetDetector zipCharsetDetector,
                                 Map<String, String> tags) throws IOException {
        super(projectName, projectFolder, userWorkspace, tags);
        this.repository = repository;
        this.comment = comment;

        uploadedFile = FileTool.toTempFile(uploadedFileStream, uploadedFileName);
        charset = zipCharsetDetector.detectCharset(new ZipFromFile(uploadedFile));
        if (charset == null) {
            throw new IOException("Cannot detect a charset for the zip file");
        }

        if (isEmptyZip(uploadedFile)) {
            throw new IOException("Cannot create a project from the given file. Zip file is empty.");
        }

        try {
            this.zipFile = new ZipFile(uploadedFile, charset);
        } catch (IOException e) {
            destroy();
            throw e;
        }
        this.zipFilter = zipFilter;
    }

    private boolean isEmptyZip(File uploadedFile) {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(new FileInputStream(uploadedFile), charset);
            if (zipInputStream.getNextEntry() == null) {
                return true;
            }
        } catch (IOException ignored) {
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
        return false;
    }

    private ZipRulesProjectBuilder getZipProjectBuilder(Set<String> sortedNames, PathFilter zipFilter) {
        var folderExtractor = new RootFolderExtractor(sortedNames, zipFilter);
        return new ZipRulesProjectBuilder(getUserWorkspace(), repository, getProjectName(),
                getProjectFolder(),
                zipFilter,
                folderExtractor,
                comment);
    }

    private Set<String> sortZipEntriesNames(ZipFile zipFile) {
        // Sort zip entries names alphabetically
        var sortedNames = new TreeSet<String>();
        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements(); ) {
            try {
                var item = items.nextElement();
                sortedNames.add(item.getName());
            } catch (Exception e) {
                log.warn("Cannot extract zip entry.", e);
            }
        }
        return sortedNames;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        var sortedNames = sortZipEntriesNames(zipFile);
        List<String> invalidNames = incorrectNames();

        if (!invalidNames.isEmpty()) {
            // The names themselves are what the uploader has to correct, so the first of them are named; a zip
            // whose every name is wrong would otherwise answer with a count and nothing to act on.
            throw new ProjectException("Zip file contains %d files/folders with incorrect names: %s. %s"
                    .formatted(invalidNames.size(),
                            String.join(", ", invalidNames.subList(0, Math.min(invalidNames.size(), 20))),
                            NameChecker.BAD_NAME_MSG));
        }

        var projectBuilder = getZipProjectBuilder(sortedNames, zipFilter);

        for (String name : sortedNames) {

            try {
                var item = zipFile.getEntry(name);

                if (item == null) {
                    throw new ProjectException("Cannot read zip entry '%s'. Possible broken zip.".formatted(name));
                }

                if (item.isDirectory()) {
                    projectBuilder.addFolder(item.getName());
                } else {
                    requireSizeWithinLimit(item);
                    InputStream zipInputStream;
                    try {
                        var fileName = projectBuilder.getFolderExtractor().extractFromRootFolder(item.getName());
                        zipInputStream = changeFileIfNeeded(fileName, zipFile.getInputStream(item));
                    } catch (IOException e) {
                        throw new ProjectException("Error extracting zip archive", e);
                    }
                    projectBuilder.addFile(item.getName(), zipInputStream);
                }
            } catch (Exception e) {
                projectBuilder.cancel();
                log.warn("Bad zip entry name [{}].", name);
                var message = e.getMessage();
                if (message == null) {
                    message = "Bad zip entry '%s'".formatted(name);
                }
                throw new ProjectException(message, e);
            }
        }
        return projectBuilder;
    }

    @Override
    public final void destroy() {
        try {
            if (zipFile != null) {
                zipFile.close();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        if (!uploadedFile.delete()) {
            log.warn("Cannot delete the file {}", uploadedFile.getName());
        }
    }

    /** Refuses a file the project cannot take, rather than leaving it out of a project reported as created. */
    private static void requireSizeWithinLimit(ZipEntry file) throws ProjectException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ProjectException("Size of the file " + file.getName() + " is more then 100MB.");
        }
    }

    /**
     * Validate if folders and files into zip archive have incorrect names
     *
     * @return List of incorrect names of folders and files
     */
    private List<String> incorrectNames() {
        var invalidNames = new LinkedList<String>();
        if (zipFile == null) {
            return invalidNames;
        }

        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements(); ) {
            try {
                var item = items.nextElement();

                if (!item.isDirectory()) {
                    String name = FileUtils.getName(item.getName());

                    if (!NameChecker.checkName(name)) {
                        invalidNames.add(name);
                    }
                } else {
                    var files = item.getName().split("/");

                    for (String folderName : files) {
                        if (!NameChecker.checkName(folderName)) {
                            invalidNames.add(folderName);
                        }
                    }
                }

            } catch (Exception e) {
                log.warn("Cannot extract zip entry.", e);
            }
        }
        return invalidNames;
    }
}
