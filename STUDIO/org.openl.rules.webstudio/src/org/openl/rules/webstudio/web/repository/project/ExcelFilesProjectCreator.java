package org.openl.rules.webstudio.web.repository.project;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.webstudio.web.repository.upload.AProjectCreator;
import org.openl.rules.webstudio.web.repository.upload.RulesProjectBuilder;
import org.openl.rules.workspace.uw.UserWorkspace;

public class ExcelFilesProjectCreator extends AProjectCreator {

    private ProjectFile[] files;

    public ExcelFilesProjectCreator(String projectName, UserWorkspace userWorkspace, ProjectFile... files) {
        super(projectName, userWorkspace);
        this.files = files;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        RulesProjectBuilder projectBuilder = new RulesProjectBuilder(getUserWorkspace(), getProjectName());

        if (files != null) {
            for (ProjectFile file : files) {
                try {
                    if (checkFileSize(file)) {
                        projectBuilder.addFile(FilenameUtils.getName(file.getName()), file.getInput());
                    }
                } catch (Exception e) {
                    FacesUtils.addErrorMessage("Problem with file " + file.getName() + ". " + e.getMessage());
                }
            }
        }

        return projectBuilder;
    }

    @Override
    public void destroy() {
        for (ProjectFile file : files) {
            IOUtils.closeQuietly(file.getInput());
        }
    }

    private boolean checkFileSize(ProjectFile file) {
        if (file.getSize() > 100 * 1024 * 1024) {
            FacesUtils.addErrorMessage("Size of the file " + file.getName() + " is more then 100MB.");
            return false;
        }
        return true;
    }

}
