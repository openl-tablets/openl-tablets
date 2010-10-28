package org.openl.rules.webstudio.web.repository.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipException;

import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTypeHelper;
import org.richfaces.model.UploadItem;

public class ProjectUploader {
    
    private static final String NAME_ALREADY_EXISTS = "Cannot create project because project with such name already exists.";

    private static final String INVALID_PROJECT_NAME = "Specified name is not a valid project name.";
    
    private UploadItem uploadedItem;
    private String projectName;
    private UserWorkspace userWorkspace;
    private PathFilter zipFilter;
   
    public ProjectUploader(UploadItem uploadItem, String projectName, UserWorkspace userWorkspace, PathFilter zipFilter) {
        super();
        this.uploadedItem = uploadItem;
        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
        this.zipFilter = zipFilter;
    }
    
    public String uploadProject() {
        String errorMessage = getProblemWithProjectName();        
        if (errorMessage == null) {
            errorMessage = createProjectFromUploadedFile(uploadedItem);
        }
        return errorMessage;
    }
    
    private String createProjectFromUploadedFile(UploadItem uploadedItem) {
        String errorMessage = null;
        AProjectCreator projectCreator;
        try {
            projectCreator = getProjectCreator(uploadedItem);
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
    
    private AProjectCreator getProjectCreator(UploadItem uploadedItem) throws ZipException, IOException, FileNotFoundException {
        AProjectCreator projectCreator = null;
        File uploadedFile = uploadedItem.getFile();
        
        if (uploadedFile != null && uploadedFile.isFile()) {
            if (FileTypeHelper.isZipFile(uploadedItem.getFileName())) {
                projectCreator = new ZipFileProjectCreator(uploadedFile, projectName, userWorkspace, zipFilter);                
            } else if (FileTypeHelper.isExcelFile(uploadedItem.getFileName())) {
                projectCreator = new ExcelFileProjectCreator(projectName, userWorkspace, new FileInputStream(uploadedFile),
                        uploadedItem.getFileName());   
            }
        }        
        return projectCreator;
    }

}
