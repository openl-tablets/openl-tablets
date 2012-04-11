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

import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;

public class ZipFileProjectCreator extends AProjectCreator {
    
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
    
    private ZipRulesProjectBuilder getZipProjectBuilder(Set<String> sortedNames) throws ProjectException {        
        RootFolderExtractor folderExtractor = new RootFolderExtractor(sortedNames);
        return new ZipRulesProjectBuilder(getUserWorkspace(), getProjectName(), zipFilter, folderExtractor);
    }
    
    private Set<String> sortZipEntriesNames(ZipFile zipFile) {
        // Sort zip entries names alphabetically
        Set<String> sortedNames = new TreeSet<String>();
        for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements();) {
            ZipEntry item = items.nextElement();
            sortedNames.add(item.getName());
        }
        return sortedNames;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        Set<String> sortedNames = sortZipEntriesNames(zipFile);
        ZipRulesProjectBuilder projectBuilder = getZipProjectBuilder(sortedNames);
        for (String name : sortedNames) {
            ZipEntry item = zipFile.getEntry(name);
            if (item.isDirectory()) {
                projectBuilder.addFolder(item.getName());
            } else {
                InputStream zipInputStream;
                try {
                    zipInputStream = zipFile.getInputStream(item);
                } catch (IOException e) {                    
                    throw new ProjectException("Error extracting zip archive", e);
                }
                projectBuilder.addFile(item.getName(), zipInputStream);
            }
        }
        return projectBuilder;
    }
}
