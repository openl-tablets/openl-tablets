package org.openl.rules.webstudio.web.repository.project;

import java.io.IOException;

import org.openl.rules.webstudio.web.repository.upload.AProjectCreator;
import org.openl.rules.webstudio.web.repository.upload.RulesProjectBuilder;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;

public class ExcelFilesProjectCreator extends AProjectCreator {

    private ProjectFile[] files;
    private PathFilter pathFilter;
    private final String comment;

    public ExcelFilesProjectCreator(String projectName,
            String projectFolder,
            UserWorkspace userWorkspace,
            String comment,
            PathFilter pathFilter,
            ProjectFile... files) {
        super(projectName, projectFolder, userWorkspace);
        this.comment = comment;
        this.pathFilter = pathFilter;
        this.files = files;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() {
        RulesProjectBuilder projectBuilder = new RulesProjectBuilder(getUserWorkspace(),
            getProjectName(),
            getProjectFolder(),
            comment);

        if (files != null) {
            for (ProjectFile file : files) {
                String fileName = file.getName();
                if (!pathFilter.accept(fileName)) {
                    continue;
                }
                try {
                    if (checkFileSize(file)) {
                        projectBuilder.addFile(fileName, changeFileIfNeeded(fileName, file.getInput()));
                    }
                } catch (Exception e) {
                    WebStudioUtils.addErrorMessage("Problem with file " + fileName + ". " + e.getMessage());
                }
            }
        }

        return projectBuilder;
    }

    @Override
    public void destroy() {
        for (ProjectFile file : files) {
            try {
                IOUtils.closeQuietly(file.getInput());
            } catch (IOException ignored) {
            }
        }
    }

    private boolean checkFileSize(ProjectFile file) {
        if (file.getSize() > 100 * 1024 * 1024) {
            WebStudioUtils.addErrorMessage("Size of the file " + file.getName() + " is more then 100MB.");
            return false;
        }
        return true;
    }

}
