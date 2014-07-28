package org.openl.rules.webstudio.web.repository.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTool;

public class ZipFileProjectCreator extends AProjectCreator {
    private final Log log = LogFactory.getLog(ZipFileProjectCreator.class);
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

        this.zipFile = new ZipFile(uploadedFile);
        this.zipFilter = zipFilter;
    }

    private ZipRulesProjectBuilder getZipProjectBuilder(Set<String> sortedNames, PathFilter zipFilter) throws ProjectException {
        RootFolderExtractor folderExtractor = new RootFolderExtractor(sortedNames, zipFilter);
        return new ZipRulesProjectBuilder(getUserWorkspace(), getProjectName(), zipFilter, folderExtractor);
    }

    private Set<String> sortZipEntriesNames(ZipFile zipFile) {
        // Sort zip entries names alphabetically
        Set<String> sortedNames = new TreeSet<String>();
        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements();) {
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
        List<String> invalidNames = inccorrectNames();

        if (invalidNames.size() > 0) {
            FacesUtils.addErrorMessage("Project was not created. Zip file containts " + invalidNames.size() + " files/folders with incorrect names:");

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
                log.warn(String.format("Bad zip entry name [%s].", name));
                throw new ProjectException(e.getMessage(), e);
            }
        }
        return projectBuilder;
    }

    @Override
    public void destroy() {
        try {
            zipFile.close();
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
        if (!uploadedFile.delete()) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Can't delete the file %s", uploadedFile.getName()));
            }
        }
    }

    private boolean checkFileSize(ZipEntry file) {
        if(file.getSize() > 100*1024*1024) {
            FacesUtils.addErrorMessage("Size of the file "+file.getName()+" is more then 100MB.");
            return false;
        }

        return true;
    }

    /**
     * Validate if folders and files into zip archive have incorrect names
     * 
     * @return List of incorrect names of folders and files
     */
    public List<String> inccorrectNames() {
        List<String> invalidNames = new LinkedList<String>();

        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements();) {
            try {
                ZipEntry item = items.nextElement();

                if (!item.isDirectory()) {
                    String name = FilenameUtils.getName(item.getName());

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
