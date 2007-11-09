package org.openl.rules.ui.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private final static Log log = LogFactory.getLog(ProjectUploadController.class);
    private UploadedFile file;
    private String projectName;
    private UploadService uploadService;
    private RepositoryTreeController repositoryTree;

    public void setRepositoryTree(RepositoryTreeController repositoryTree) {
        this.repositoryTree = repositoryTree;
    }

    public String execute() {
        if (upload()) {
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
        params.setProjectName(projectName);

        RulesUserSession rulesUserSession = (RulesUserSession) FacesUtils.getSessionMap()
                .get("rulesUserSession");

        try {
            UserWorkspace workspace = rulesUserSession.getUserWorkspace();
            params.setWorkspace(workspace);
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
            return false;
        }

        try {
            UploadServiceResult result = (UploadServiceResult) uploadService.execute(params);

            //importFile = result.getResultFile().getName();
        } catch (ServiceException e) {
            log.error("Error while uploading project", e);
            String message = "Error occured during uploading file";
            FacesMessage facesMessage = null;
            facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            FacesContext.getCurrentInstance().addMessage(null, facesMessage);
            return false;
        }

        repositoryTree.reInit();
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
