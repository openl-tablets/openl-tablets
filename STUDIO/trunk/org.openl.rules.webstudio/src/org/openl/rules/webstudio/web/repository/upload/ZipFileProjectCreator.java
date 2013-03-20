package org.openl.rules.webstudio.web.repository.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.richfaces.model.UploadedFile;

public class ZipFileProjectCreator extends AProjectCreator {
    private final Log log = LogFactory.getLog(ZipFileProjectCreator.class);
    
    private ZipFile zipFile;
    
    private PathFilter zipFilter;
    
    public ZipFileProjectCreator(File uploadedFile,
            String projectName,
            UserWorkspace userWorkspace,
            PathFilter zipFilter) throws ZipException, IOException{
        super(projectName, userWorkspace);
        
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
                            zipInputStream = zipFile.getInputStream(item);
                        } catch (IOException e) {
                            throw new ProjectException("Error extracting zip archive", e);
                        }
                        projectBuilder.addFile(item.getName(), zipInputStream);
                    }
                }
            } catch (Exception e) {
                // TODO message on UI
                log.warn(String.format("Bad zip entry name [%s].", name));
            }
        }
        return projectBuilder;
    }
    
    private boolean checkFileSize(ZipEntry file) {
        if(file.getSize() > 100*1024*1024) {
            FacesUtils.addErrorMessage("Size of the file "+file.getName()+" is more then 100MB.");
            return false;
        }
        
        return true;
    }
}
