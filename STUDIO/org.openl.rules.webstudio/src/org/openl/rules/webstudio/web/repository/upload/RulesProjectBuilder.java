package org.openl.rules.webstudio.web.repository.upload;

import java.io.InputStream;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesProjectBuilder {
    private final Logger log = LoggerFactory.getLogger(RulesProjectBuilder.class);
    private final RulesProject project;
    private final UserWorkspace workspace;
    private final String internalPath;
    private final String comment;

    public RulesProjectBuilder(UserWorkspace workspace, String projectName, String projectFolder, String comment) throws ProjectException {
        this.workspace = workspace;
        this.comment = comment;
        internalPath = projectFolder + projectName;
        synchronized (this.workspace) {
            project = workspace.createProject(projectName);
        }
    }

    protected RulesProject getProject() {
        return project;
    }

    public boolean addFile(String fileName, InputStream inputStream) throws ProjectException {
        AProjectFolder folder = project;
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

        folder.addResource(resName, inputStream);

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
            log.debug("Canceling uploading of new project");

            synchronized (workspace) {
                project.close();
                project.erase(workspace.getUser(), comment);
            }
        } catch (ProjectException e) {
            log.error("Failed to cancel new project", e);
        }
    }

    public void save() throws ProjectException {
        WorkspaceUser user = workspace.getUser();
        Repository designRepository = project.getDesignRepository();
        if (designRepository.supports().mappedFolders()) {
            project.getFileData().addAdditionalData(new FileMappingData(internalPath));
        }

        // Override comment to avoid reusing of comment from previous version (we create a new project but it can contain
        // unerasable history for example in Git).
        project.getFileData().setComment(comment);
        project.save(user);
        workspace.refresh();
    }

    private void checkName(String artefactName) throws ProjectException {
        if (!NameChecker.checkName(artefactName)) {
            throw new ProjectException(
                "File or folder name '" + artefactName + "' is invalid. " + NameChecker.BAD_NAME_MSG);
        }
    }

    private AProjectFolder checkPath(AProject project, String fullName) throws ProjectException {
        ArtefactPathImpl ap = new ArtefactPathImpl(fullName);
        AProjectFolder current = project;
        for (String segment : ap.getSegments()) {
            if (current.hasArtefact(segment)) {
                current = (AProjectFolder) current.getArtefact(segment);
            } else {
                // throws exception if name is invalid
                checkName(segment);

                current = current.addFolder(segment);
            }
        }

        return current;
    }
}
