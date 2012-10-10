package org.openl.rules.webstudio.web.repository.upload;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.richfaces.model.UploadedFile;

public class ExcelFileProjectCreator extends AProjectCreator {
    
    private InputStream rulesSource;
    private String rulesSourceName;
    private List<UploadedFile> uploadedFiles;
    
    public ExcelFileProjectCreator(String projectName, UserWorkspace userWorkspace,
            InputStream rulesSource, String rulesSourceName) {
        super(projectName, userWorkspace);
        this.rulesSource = rulesSource;
        this.rulesSourceName = rulesSourceName;
    }

    public ExcelFileProjectCreator(String projectName, UserWorkspace userWorkspace, List<UploadedFile> uploadedFiles) {
        super(projectName, userWorkspace);
        this.uploadedFiles = uploadedFiles;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        RulesProjectBuilder projectBuilder = new RulesProjectBuilder(getUserWorkspace(), getProjectName());

        if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
            for (UploadedFile file : uploadedFiles) {
                try {
                    projectBuilder.addFile(FilenameUtils.getName(file.getName()), file.getInputStream());
                } catch (Exception e) {
                    FacesUtils.addWarnMessage("Problem with file "+file.getName()+". "+e.getMessage());
                }
            }
        } else {
            projectBuilder.addFile(rulesSourceName, rulesSource);
        }
        
        return projectBuilder;
    }

}
