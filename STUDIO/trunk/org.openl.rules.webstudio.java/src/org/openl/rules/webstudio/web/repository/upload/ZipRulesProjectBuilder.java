package org.openl.rules.webstudio.web.repository.upload;

import java.io.InputStream;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * Project builder for projects uploaded as zip files.
 * 
 * @author DLiauchuk
 *
 */
public class ZipRulesProjectBuilder extends RulesProjectBuilder {
    
    private RootFolderExtractor folderExtractor;
    
    public ZipRulesProjectBuilder(UserWorkspace workspace, String projectName, PathFilter filter, RootFolderExtractor folderExtractor) throws ProjectException {
        super(workspace, projectName, filter);
        this.folderExtractor = folderExtractor;
    }
    
    @Override
    public boolean addFile(String fileName, InputStream inputStream) throws ProjectException {
        String filteredFileName = folderExtractor.extractFromRootFolder(fileName);
        if (filteredFileName != null) {
            return super.addFile(filteredFileName, inputStream);
        }
        return false;
        
    }
    
    @Override
    public boolean addFolder(String folderName) throws ProjectException {
        String filteredFolderName = folderExtractor.extractFromRootFolder(folderName);
        if (filteredFolderName != null) {
            return super.addFolder(filteredFolderName);
        }
        return false;
        
    }

}
