package org.openl.rules.webstudio.web.repository.upload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.project.ExcelFilesProjectCreator;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTypeHelper;
import org.richfaces.model.UploadedFile;

public class ProjectUploader {

    private static final String NAME_ALREADY_EXISTS = "Cannot create project because project with such name already exists.";

    private static final String INVALID_PROJECT_NAME = "Specified name is not a valid project name.";

    private String projectName;
    private UserWorkspace userWorkspace;
    private PathFilter zipFilter;
    private List<UploadedFile> uploadedFiles;

    public ProjectUploader(UploadedFile uploadedFile, String projectName, UserWorkspace userWorkspace, PathFilter zipFilter) {
        super();
        this.uploadedFiles = new ArrayList<UploadedFile>();
        this.uploadedFiles.add(uploadedFile);
        
        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
        this.zipFilter = zipFilter;
    }
    
    public ProjectUploader(List<UploadedFile> uploadedFiles, String projectName, UserWorkspace userWorkspace,
            PathFilter zipFilter) {
        super();
        this.uploadedFiles = uploadedFiles;
        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
        this.zipFilter = zipFilter;
    }

    public String uploadProject() {
        String errorMessage = getProblemWithProjectName();        
        if (errorMessage == null) {
            errorMessage = createProjectFromUploadedFile(uploadedFiles);
        }
        return errorMessage;
    }

    private String createProjectFromUploadedFile(List<UploadedFile> uploadedFiles) {
        String errorMessage = null;
        AProjectCreator projectCreator = null;
        try {
            projectCreator = getProjectCreator(uploadedFiles);
            if (projectCreator != null) {
                errorMessage = projectCreator.createRulesProject();
            } else {
                errorMessage = "Can`t create project from given file.";
            }
        } catch (IOException e) {
            errorMessage = e.getMessage();
        } finally {
            if (projectCreator != null) {
                projectCreator.destroy();
            }
        }
        return errorMessage;
    }

    private String getProblemWithProjectName() {
        String problem = null;
        if (!NameChecker.checkName(projectName)) {
            problem = INVALID_PROJECT_NAME + " " + NameChecker.BAD_NAME_MSG;
        } else if (userWorkspace.hasProject(projectName)) {
            problem = NAME_ALREADY_EXISTS;
        } 
        return problem;
    }

    private AProjectCreator getProjectCreator(List<UploadedFile> uploadedFiles) throws IOException {
        AProjectCreator projectCreator = null;

        if (!uploadedFiles.isEmpty()) {
            if (FileTypeHelper.isZipFile(FilenameUtils.getName(getLastElement().getName()))) {
                /*Create project creator for single zip file*/
                String file = FilenameUtils.getName(getLastElement().getName());
                projectCreator = new ZipFileProjectCreator(file, getLastElement().getInputStream(), projectName, userWorkspace, zipFilter);
            } else {
                List<ProjectFile> projectFiles = new ArrayList<ProjectFile>();
                for (UploadedFile uploadedFile : uploadedFiles) {
                    projectFiles.add(new ProjectFile(
                            uploadedFile.getName(), uploadedFile.getInputStream(), uploadedFile.getSize()));
                }
                projectCreator = new ExcelFilesProjectCreator(projectName, userWorkspace,
                        projectFiles.toArray(new ProjectFile[uploadedFiles.size()]));
            }

        }
        return projectCreator;
    }

    private UploadedFile getLastElement() {
        if (!uploadedFiles.isEmpty()) {
            return uploadedFiles.get(uploadedFiles.size() - 1);
        } else {
            return null;
        }
    }

}
