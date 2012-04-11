package org.openl.rules.webstudio.web.repository.upload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.FileProjectResource;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;

import java.io.InputStream;

public class RulesProjectBuilder {
    private static final Log LOG = LogFactory.getLog(RulesProjectBuilder.class);
    private final UserWorkspaceProject project;
    

    public RulesProjectBuilder(UserWorkspace workspace, String projectName) throws ProjectException {
        workspace.createProject(projectName);
        project = workspace.getProject(projectName);
        project.checkOut();
    }

    public boolean addFile(String fileName, InputStream inputStream) throws ProjectException {
        UserWorkspaceProjectFolder folder = project;
        String resName;

        int pos = fileName.lastIndexOf('/');
        if (pos >= 0) {
            String path = fileName.substring(0, pos);
            resName = fileName.substring(pos + 1);

            folder = checkPath(project, path);
        } else {
            resName = fileName;
        }

        // throws exception if name is invalid
        checkName(resName);

        ProjectResource projectResource = new FileProjectResource(inputStream);
        folder.addResource(resName, projectResource);

        return true;
    }

    public boolean addFolder(String folderName) throws ProjectException {        
        folderName = folderName.substring(0, folderName.length() - 1);

        checkPath(project, folderName);

        return true;
    }

    public void cancel() {
        // it was created it will be perish
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Canceling uploading of new project");
            }

            project.close();
            project.delete();
            project.erase();
        } catch (ProjectException e) {
            LOG.error("Failed to cancel new project", e);
        }
    }

    public void checkIn() throws ProjectException {
        project.checkIn();
    }

    private void checkName(String artefactName) throws ProjectException {
        if (!NameChecker.checkName(artefactName)) {
            throw new ProjectException("File or folder name '" + artefactName + "' is invalid. "
                    + NameChecker.BAD_NAME_MSG);
        }
    }

    private UserWorkspaceProjectFolder checkPath(UserWorkspaceProject project, String fullName) throws ProjectException {
        ArtefactPathImpl ap = new ArtefactPathImpl(fullName);
        UserWorkspaceProjectFolder current = project;
        for (String segment : ap.getSegments()) {
            if (current.hasArtefact(segment)) {
                current = (UserWorkspaceProjectFolder) current.getArtefact(segment);
            } else {
                // throws exception if name is invalid
                checkName(segment);

                current = current.addFolder(segment);
            }
        }

        return current;
    }
}
