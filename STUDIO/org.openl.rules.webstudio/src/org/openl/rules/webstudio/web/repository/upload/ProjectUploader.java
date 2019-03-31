package org.openl.rules.webstudio.web.repository.upload;

import org.openl.rules.webstudio.web.repository.project.ExcelFilesProjectCreator;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTypeHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectUploader {
    private String projectName;
    private UserWorkspace userWorkspace;
    private PathFilter zipFilter;
    private List<ProjectFile> uploadedFiles;
    private final ZipCharsetDetector zipCharsetDetector;

    public ProjectUploader(ProjectFile uploadedFile,
            String projectName,
            UserWorkspace userWorkspace,
            PathFilter zipFilter,
            ZipCharsetDetector zipCharsetDetector) {
        this.zipCharsetDetector = zipCharsetDetector;
        this.uploadedFiles = new ArrayList<>();
        this.uploadedFiles.add(uploadedFile);

        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
        this.zipFilter = zipFilter;
    }

    public ProjectUploader(List<ProjectFile> uploadedFiles,
            String projectName,
            UserWorkspace userWorkspace,
            PathFilter zipFilter,
            ZipCharsetDetector zipCharsetDetector) {
        this.uploadedFiles = uploadedFiles;
        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
        this.zipFilter = zipFilter;
        this.zipCharsetDetector = zipCharsetDetector;
    }

    public String uploadProject() {
        String errorMessage;
        AProjectCreator projectCreator = null;
        if (uploadedFiles.isEmpty()) {
            errorMessage = "Can`t create project from the given file.";
        } else
            try {
                // Get the last file
                ProjectFile file = uploadedFiles.get(uploadedFiles.size() - 1);
                String fileName = file.getName();
                if (FileTypeHelper.isZipFile(fileName)) {
                    // Create project creator for the single zip file
                    projectCreator = new ZipFileProjectCreator(fileName,
                        file.getInput(),
                        projectName,
                        userWorkspace,
                        zipFilter,
                        zipCharsetDetector);
                } else {
                    projectCreator = new ExcelFilesProjectCreator(projectName,
                        userWorkspace,
                        zipFilter,
                        uploadedFiles.toArray(new ProjectFile[0]));
                }
                errorMessage = projectCreator.createRulesProject();
            } catch (IOException e) {
                errorMessage = e.getMessage();
            } catch (Exception e) {
                errorMessage = "Can`t create project. Error: " + e.getMessage();
            } finally {
                if (projectCreator != null) {
                    projectCreator.destroy();
                }
            }
        return errorMessage;
    }
}
