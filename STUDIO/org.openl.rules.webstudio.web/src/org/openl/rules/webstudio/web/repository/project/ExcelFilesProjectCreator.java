package org.openl.rules.webstudio.web.repository.project;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.repository.upload.AProjectCreator;
import org.openl.rules.webstudio.web.repository.upload.RulesProjectBuilder;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;

/** Creates a project from Excel files, arranging them in the standard layout when no descriptor is supplied. */
public class ExcelFilesProjectCreator extends AProjectCreator {

    private final ProjectFile[] files;
    private final Repository repository;
    private final PathFilter pathFilter;
    private final String comment;

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

        try {
            var acceptedFiles = files == null ? List.<ProjectFile>of()
                    : Arrays.stream(files).filter(file -> pathFilter.accept(file.getName())).toList();
            var useDefaultLayout = acceptedFiles.stream()
                    .noneMatch(file -> ProjectDescriptor.FILE_NAME.equals(file.getName()));
            if (useDefaultLayout) {
                projectBuilder.addFile(ProjectDescriptor.FILE_NAME,
                        new ByteArrayInputStream(DefaultProjectLayout.rulesXml(acceptedFiles.stream()
                                .map(ProjectFile::getName)
                                .map(DefaultProjectLayout::filePath)
                                .toList())));
            }
            for (ProjectFile file : acceptedFiles) {
                addFile(projectBuilder, file, useDefaultLayout);
            }
        } catch (RuntimeException | ProjectException e) {
            projectBuilder.cancel();
            throw e;
        }

        return projectBuilder;
    }

    private void addFile(RulesProjectBuilder projectBuilder, ProjectFile file, boolean useDefaultLayout)
            throws ProjectException {
        if (!checkFileSize(file)) {
            throw new ProjectException("Size of the file " + file.getName() + " is more then 100MB.");
        }
        var fileName = file.getName();
        try {
            projectBuilder.addFile(projectPath(fileName, useDefaultLayout),
                    changeFileIfNeeded(fileName, file.getInput()));
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        }
    }

    private static String projectPath(String fileName, boolean useDefaultLayout) {
        return useDefaultLayout ? DefaultProjectLayout.filePath(fileName) : fileName;
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
