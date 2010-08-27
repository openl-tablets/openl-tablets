package org.openl.rules.webstudio.web.repository;

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
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;

public class ZipFileProjectCreator extends AProjectCreator {
    
    private static final Log LOG = LogFactory.getLog(ZipFileProjectCreator.class);
    
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

    @Override
    public String createRulesProject() {
        String errorMessage = null;
        RulesProjectBuilder projectBuilder = null;
        try {
            projectBuilder = new RulesProjectBuilder(getUserWorkspace(), getProjectName(), zipFilter);

            Set<String> sortedNames = sortZipEntriesNames(zipFile);

            for (String name : sortedNames) {
                ZipEntry item = zipFile.getEntry(name);
                if (item.isDirectory()) {
                    projectBuilder.addFolder(item.getName());
                } else {
                    InputStream zipInputStream = zipFile.getInputStream(item);
                    projectBuilder.addFile(item.getName(), zipInputStream);
                }
            }
            projectBuilder.checkIn();
        } catch (Exception e) {
            if (projectBuilder != null) {
                projectBuilder.cancel();
            }
            LOG.error("Error creating project.", e);
            errorMessage = e.getMessage();
        }

        return errorMessage;
        
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

}
