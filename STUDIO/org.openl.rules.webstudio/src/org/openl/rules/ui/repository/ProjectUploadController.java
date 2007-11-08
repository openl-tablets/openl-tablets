package org.openl.rules.ui.repository;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.openl.rules.webstudio.RulesUserSession;
import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.webstudio.services.upload.UploadService;
import org.openl.rules.webstudio.services.upload.UploadServiceParams;
import org.openl.rules.webstudio.services.upload.UploadServiceResult;
import org.openl.rules.webstudio.util.FacesUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.PropertyTypeException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;


/**
 * Project upload controller.
 *
 * @author Andrey Naumenko
 */
public class ProjectUploadController {
    private UploadedFile file;
    private String projectName;
    private UploadService uploadService;
    private boolean uploadSuccessful = false;

    public String execute() {
        // TODO set service
        if (upload()) {
            uploadSuccessful = true;

            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("File was successfully uploaded"));
            return "success";
        } else {
            return "";
        }
    }

    private boolean upload() {
        UploadServiceParams params = new UploadServiceParams();
        params.setFile(file);
        try {
            UploadServiceResult result = (UploadServiceResult) uploadService
                    .execute(params);

            //importFile = result.getResultFile().getName();
        } catch (ServiceException e) {
            String message = "Error occured during uploading file";
            FacesMessage facesMessage = null;
            facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            FacesContext.getCurrentInstance().addMessage(null, facesMessage);
            return false;
        }

        return true;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setUploadService(UploadService uploadService) {
        this.uploadService = uploadService;
    }
}
