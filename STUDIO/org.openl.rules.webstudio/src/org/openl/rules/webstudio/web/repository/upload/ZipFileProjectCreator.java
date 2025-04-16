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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.common.ProjectException;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipFromFile;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

public class ZipFileProjectCreator extends AProjectCreator {
    private final Logger log = LoggerFactory.getLogger(ZipFileProjectCreator.class);
    private final ZipFile zipFile;
    private final PathFilter zipFilter;
    private final File uploadedFile;
    private final Charset charset;
    private final String comment;
    private final String repositoryId;

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
        super(projectName, projectFolder, userWorkspace, tags);
        this.repositoryId = repositoryId;
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
        RootFolderExtractor folderExtractor = new RootFolderExtractor(sortedNames, zipFilter);
        return new ZipRulesProjectBuilder(getUserWorkspace(), repositoryId, getProjectName(),
                getProjectFolder(),
                zipFilter,
                folderExtractor,
                comment);
    }

    private Set<String> sortZipEntriesNames(ZipFile zipFile) {
        // Sort zip entries names alphabetically
        Set<String> sortedNames = new TreeSet<>();
        if (zipFile == null) {
            return sortedNames;
        }

        boolean skipped = false;
        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements(); ) {
            try {
                ZipEntry item = items.nextElement();
                sortedNames.add(item.getName());
            } catch (Exception e) {
                log.warn("Cannot extract zip entry.", e);
                skipped = true;
            }
        }
        if (skipped) {
            WebStudioUtils.addWarnMessage("Warning: Some malformed zip entries were skipped.");
        }
        return sortedNames;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        Set<String> sortedNames = sortZipEntriesNames(zipFile);
        List<String> invalidNames = incorrectNames();

        if (!invalidNames.isEmpty()) {
            WebStudioUtils.addErrorMessage("Project has not been created. Zip file contains " + invalidNames
                    .size() + " files/folders with incorrect names:");

            /*
             * Display first 20 files/folders with incorrect names
             */
            for (int i = 0; i < Math.min(invalidNames.size(), 20); i++) {
                WebStudioUtils.addErrorMessage(invalidNames.get(i));
            }
            throw new ProjectException(NameChecker.BAD_NAME_MSG);
        }

        ZipRulesProjectBuilder projectBuilder = getZipProjectBuilder(sortedNames, zipFilter);

        for (String name : sortedNames) {

            try {
                ZipEntry item = zipFile.getEntry(name);

                if (item == null) {
                    throw new ProjectException(String.format("Cannot read zip entry '%s'. Possible broken zip.", name));
                }

                if (item.isDirectory()) {
                    projectBuilder.addFolder(item.getName());
                } else {
                    if (checkFileSize(item)) {
                        InputStream zipInputStream;
                        try {
                            String fileName = projectBuilder.getFolderExtractor().extractFromRootFolder(item.getName());
                            zipInputStream = changeFileIfNeeded(fileName, zipFile.getInputStream(item));
                        } catch (IOException e) {
                            throw new ProjectException("Error extracting zip archive", e);
                        }
                        projectBuilder.addFile(item.getName(), zipInputStream);
                    }
                }
            } catch (Exception e) {
                projectBuilder.cancel();
                log.warn("Bad zip entry name [{}].", name);
                String message = e.getMessage();
                if (message == null) {
                    message = String.format("Bad zip entry '%s'", name);
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

    private boolean checkFileSize(ZipEntry file) {
        if (file.getSize() > 1000 * 1024 * 1024) {
            WebStudioUtils.addErrorMessage("Size of the file " + file.getName() + " is more then 100MB.");
            return false;
        }

        return true;
    }

    /**
     * Validate if folders and files into zip archive have incorrect names
     *
     * @return List of incorrect names of folders and files
     */
    private List<String> incorrectNames() {
        List<String> invalidNames = new LinkedList<>();
        if (zipFile == null) {
            return invalidNames;
        }

        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements(); ) {
            try {
                ZipEntry item = items.nextElement();

                if (!item.isDirectory()) {
                    String name = FileUtils.getName(item.getName());

                    if (!NameChecker.checkName(name)) {
                        invalidNames.add(name);
                    }
                } else {
                    if (!StringUtils.containsIgnoreCase(item.getName(), FolderHelper.PROPERTIES_FOLDER)) {

                        String[] files = item.getName().split("/");

                        for (String folderName : files) {
                            if (!NameChecker.checkName(folderName)) {
                                invalidNames.add(folderName);
                            }
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
