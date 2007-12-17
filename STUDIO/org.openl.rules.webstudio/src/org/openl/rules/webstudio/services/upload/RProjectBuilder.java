package org.openl.rules.webstudio.services.upload;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;

import java.io.InputStream;

public class RProjectBuilder {
    private final UserWorkspaceProject project;
    private final UploadFilter filter = FolderUploadFilter.VCS_FILES_FILTER;

    public RProjectBuilder(UserWorkspace workspace, String projectName) throws ProjectException {
        workspace.createProject(projectName);
        project = workspace.getProject(projectName);
        project.checkOut();
    }

    public boolean addFile(String fileName, InputStream inputStream) throws ProjectException {
        if (!filter.accept(fileName)) {
            return false;
        }

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

        ProjectResource projectResource = new FileProjectResource(inputStream);
        folder.addResource(resName, projectResource);

        return true;
    }

    public boolean addFolder(String folderName) throws ProjectException {
        if (!filter.accept(folderName)) {
            return false;
        }
        folderName = folderName.substring(0, folderName.length() - 1);

        checkPath(project, folderName);

        return true;
    }


    public void checkIn() throws ProjectException {
        project.checkIn();
    }

    private UserWorkspaceProjectFolder checkPath(UserWorkspaceProject project, String fullName) throws ProjectException {
        ArtefactPathImpl ap = new ArtefactPathImpl(fullName);
        UserWorkspaceProjectFolder current = project;
        for (String segment : ap.getSegments()) {
            if (current.hasArtefact(segment)) {
                current = (UserWorkspaceProjectFolder) current.getArtefact(segment);
            } else {
                current = current.addFolder(segment);
            }
        }

        return current;
    }
}
