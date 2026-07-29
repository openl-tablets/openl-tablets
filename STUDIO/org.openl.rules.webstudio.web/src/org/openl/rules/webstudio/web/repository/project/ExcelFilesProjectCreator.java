package org.openl.rules.webstudio.web.repository.project;

import java.io.IOException;
import java.util.Map;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.repository.upload.AProjectCreator;
import org.openl.rules.webstudio.web.repository.upload.RulesProjectBuilder;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;

public class ExcelFilesProjectCreator extends AProjectCreator {

    private final ProjectFile[] files;
    private final Repository repository;
    private final PathFilter pathFilter;
    private final String comment;

    public ExcelFilesProjectCreator(String repositoryId,
                                    String projectName,
                                    String projectFolder,
                                    UserWorkspace userWorkspace,
                                    String comment,
                                    PathFilter pathFilter,
                                    Map<String, String> tags,
                                    ProjectFile... files) {
        this(userWorkspace.getDesignTimeRepository().getRepository(repositoryId),
                projectName,
                projectFolder,
                userWorkspace,
                comment,
                pathFilter,
                tags,
                files);
    }

    public ExcelFilesProjectCreator(Repository repository,
                                    String projectName,
                                    String projectFolder,
                                    UserWorkspace userWorkspace,
                                    String comment,
                                    PathFilter pathFilter,
                                    Map<String, String> tags,
                                    ProjectFile... files) {
        super(projectName, projectFolder, userWorkspace, tags);
        this.repository = repository;
        this.comment = comment;
        this.pathFilter = pathFilter;
        this.files = files;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        var projectBuilder = new RulesProjectBuilder(getUserWorkspace(), repository,
                getProjectName(),
                getProjectFolder(),
                comment);

        if (files != null) {
            for (ProjectFile file : files) {
                try {
                    var fileName = file.getName();
                    if (!pathFilter.accept(fileName)) {
                        continue;
                    }

                    if (checkFileSize(file)) {
                        try {
                            projectBuilder.addFile(fileName, changeFileIfNeeded(fileName, file.getInput()));
                        } catch (IOException e) {
                            throw new ProjectException(e.getMessage(), e);
                        }
                    } else {
                        throw new ProjectException("Size of the file " + file.getName() + " is more then 100MB.");
                    }
                } catch (Exception e) {
                    projectBuilder.cancel();
                    throw e;
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
                // safe to ignore: best-effort cleanup of uploaded file streams
            }
        }
    }

    private boolean checkFileSize(ProjectFile file) {
        return file.getSize() <= 1000 * 1024 * 1024;
    }

}
