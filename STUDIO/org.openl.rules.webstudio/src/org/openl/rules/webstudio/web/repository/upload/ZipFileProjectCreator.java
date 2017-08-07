package org.openl.rules.webstudio.web.repository.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipFileProjectCreator extends AProjectCreator {
    private final Logger log = LoggerFactory.getLogger(ZipFileProjectCreator.class);
    private ZipFile zipFile;
    private PathFilter zipFilter;
    private File uploadedFile;

    public ZipFileProjectCreator(String uploadedFileName,
                                 InputStream uploadedFileStream,
                                 String projectName,
                                 UserWorkspace userWorkspace,
                                 PathFilter zipFilter) throws IOException{
        super(projectName, userWorkspace);

        uploadedFile = FileTool.toTempFile(uploadedFileStream, uploadedFileName);
        
        if (isEmptyZip(uploadedFile)) {
            throw new IOException("Can`t create project from the given file. Zip file is empty!");
        }

        try {
            this.zipFile = new ZipFile(uploadedFile);
        } catch (IOException e) {
            destroy();
            throw e;
        }
        this.zipFilter = zipFilter;
    }

    private boolean isEmptyZip(File uploadedFile) {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(new FileInputStream(uploadedFile));
            if (zipInputStream.getNextEntry() == null) {
                return true;
            }
        } catch (IOException ignored) {
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
        return false;
    }

    private ZipRulesProjectBuilder getZipProjectBuilder(Set<String> sortedNames, PathFilter zipFilter) throws ProjectException {
        RootFolderExtractor folderExtractor = new RootFolderExtractor(sortedNames, zipFilter);
        return new ZipRulesProjectBuilder(getUserWorkspace(), getProjectName(), zipFilter, folderExtractor);
    }

    private Set<String> sortZipEntriesNames(ZipFile zipFile) {
        // Sort zip entries names alphabetically
        Set<String> sortedNames = new TreeSet<String>();
        if (zipFile == null) {
            return sortedNames;
        }
        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements(); ) {
            try {
                ZipEntry item = items.nextElement();
                sortedNames.add(item.getName());
            } catch (Exception e) {
                // TODO message on UI
                log.warn("Can not extract zip entry.", e);
            }
        }
        return sortedNames;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        Set<String> sortedNames = sortZipEntriesNames(zipFile);
        List<String> invalidNames = incorrectNames();

        if (invalidNames.size() > 0) {
            FacesUtils.addErrorMessage("Project was not created. Zip file contains " + invalidNames.size() + " files/folders with incorrect names:");

            /*
             * Display first 20 files/folders with incorrect names
             */
            for (int i = 0; i < (invalidNames.size() < 20 ? invalidNames.size() : 20); i++) {
                FacesUtils.addErrorMessage(invalidNames.get(i));
            }
            throw new ProjectException(NameChecker.BAD_NAME_MSG);
        }

        ZipRulesProjectBuilder projectBuilder = getZipProjectBuilder(sortedNames, zipFilter);

        for (String name : sortedNames) {

            try {
                ZipEntry item = zipFile.getEntry(name);

                if (item == null) {
                    throw new ProjectException(String.format("Can't read zip entry '%s'. Possible broken zip.", name));
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
            log.warn("Can't delete the file {}", uploadedFile.getName());
        }
    }

    private boolean checkFileSize(ZipEntry file) {
        if (file.getSize() > 100 * 1024 * 1024) {
            FacesUtils.addErrorMessage("Size of the file " + file.getName() + " is more then 100MB.");
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
        List<String> invalidNames = new LinkedList<String>();
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
                // TODO message on UI
                log.warn("Can not extract zip entry.", e);
            }
        }
        return invalidNames;
    }
}
