package org.openl.rules.ui.repository;

import org.apache.myfaces.custom.fileupload.UploadedFile;


import org.openl.rules.webstudio.RulesUserSession;
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


/**
 * Project upload controller.
 *
 * @author Andrey Naumenko
 */
public class ProjectUploadController {
    private UploadedFile file;
    private String projectName;

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

    public String upload() throws WorkspaceException, ProjectException, IOException {
        System.out.println(file.getName());
        //File.cre

        /*ZipFile zipFile = new ZipFile(file.getInputStream());

        ZipInputStream zis = new ZipInputStream();
         zis.get


        RulesUserSession rulesUserSession = (RulesUserSession) FacesUtils.getSessionMap()
                .get("rulesUserSession");
        UserWorkspace workspace = rulesUserSession.getUserWorkspace();

        projectName = "888";
        workspace.createProject(project Name);
        UserWorkspaceProject project = workspace.getProject(projectName);
        project.checkOut();
        UserWorkspaceProjectFolder folder = project.addFolder("111");
        project.checkIn();

        //project.

        //file.getInputStream();*/
        return null;
    }
}
