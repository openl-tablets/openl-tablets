package org.openl.rules.webstudio.web.repository.upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipException;

import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTool;
import org.openl.util.FileTypeHelper;
import org.richfaces.model.UploadedFile;

public class ProjectUploader {

    private static final String NAME_ALREADY_EXISTS = "Cannot create project because project with such name already exists.";

    private static final String INVALID_PROJECT_NAME = "Specified name is not a valid project name.";

    private UploadedFile uploadedFile;
    private String projectName;
    private UserWorkspace userWorkspace;
    private PathFilter zipFilter;

    public ProjectUploader(UploadedFile uploadedFile, String projectName, UserWorkspace userWorkspace, PathFilter zipFilter) {
        super();
        this.uploadedFile = uploadedFile;
        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
        this.zipFilter = zipFilter;
    }
    
    public String uploadProject() {
        String errorMessage = getProblemWithProjectName();        
        if (errorMessage == null) {
            errorMessage = createProjectFromUploadedFile(uploadedFile);
        }
        return errorMessage;
    }
    
    private String createProjectFromUploadedFile(UploadedFile uploadedFile) {
        String errorMessage = null;
        AProjectCreator projectCreator;
        try {
            projectCreator = getProjectCreator(uploadedFile);
            if (projectCreator != null) {
                errorMessage = projectCreator.createRulesProject();
            } else {
                errorMessage = "Can`t create project from given file.";
            }
        } catch (ZipException e) {                    
            errorMessage = e.getMessage();
        } catch (FileNotFoundException e) {                    
            errorMessage = e.getMessage();
        } catch (IOException e) {                    
            errorMessage = e.getMessage();
        }
        return errorMessage;
    }
    
    private String getProblemWithProjectName() {
        String problem = null;
        if (userWorkspace.hasProject(projectName)) {
            problem = NAME_ALREADY_EXISTS;
        } else if (!NameChecker.checkName(projectName)) {
            problem = INVALID_PROJECT_NAME;
        }
        return problem;
    }

    private AProjectCreator getProjectCreator(UploadedFile uploadedFile) throws ZipException, IOException, FileNotFoundException {
        AProjectCreator projectCreator = null;

        if (uploadedFile != null) {
            if (FileTypeHelper.isZipFile(uploadedFile.getName())) {
                File projectFile = FileTool.toFile(
                        uploadedFile.getInputStream(), uploadedFile.getName());
                projectCreator = new ZipFileProjectCreator(projectFile, projectName, userWorkspace, zipFilter);
            } else if (FileTypeHelper.isExcelFile(uploadedFile.getName())) {
                projectCreator = new ExcelFileProjectCreator(projectName, userWorkspace,
                        uploadedFile.getInputStream(), uploadedFile.getName());
            }
        }        
        return projectCreator;
    }

}
