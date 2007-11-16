package org.openl.rules.workspace.deploy.impl.jcr;

import org.openl.SmartProps;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.openl.rules.workspace.lw.impl.LocalProjectImpl;

import java.io.File;
import java.util.Collection;

public class JcrProductionDeployer implements ProductionDeployer {
    public static final String PROPNAME_ZIPFOLDER = "temp.zip.location";
    public static final String DEFAULT_ZIPFOLDER = "/tmp/rules-deployment/";
    public static final String WORKING_FOLDER = "work";

    private final File workingFolder;
    private final File userFolder;

    private final DeployerLocalWorkspace localWorkspace;

    public JcrProductionDeployer(WorkspaceUser user, SmartProps props) throws DeploymentException {
        String location = props.getStr(PROPNAME_ZIPFOLDER, DEFAULT_ZIPFOLDER);
        userFolder = new File(location, user.getUserId());
        workingFolder = new File(userFolder, WORKING_FOLDER);

        ensureWorkingFolderExists();

        localWorkspace = new DeployerLocalWorkspace(user, workingFolder);
    }

    private void ensureWorkingFolderExists() throws DeploymentException {
        if (!FolderHelper.checkOrCreateFolder(workingFolder)) {
            throw new DeploymentException("can not create temp folder: {0}", null, workingFolder);
        }
    }

    public DeployID deploy(Collection<Project> projects) throws DeploymentException {
        return deploy(null, projects);
    }

    public synchronized DeployID deploy(DeployID id, Collection<Project> projects) throws DeploymentException {
        if (!FolderHelper.clearFolder(userFolder)) {
            throw new DeploymentException("could not delete folder; {0}", null, userFolder);
        }
        ensureWorkingFolderExists();

        downloadProjects(projects);

        return id;
    }

    protected void downloadProjects(Collection<Project> projects) throws DeploymentException {
        for (Project project : projects) {
            try {
                localWorkspace.downloadProject(project);
            } catch (ProjectException e) {
                throw new DeploymentException("failure downloading project", e);
            }
        }
    }

    protected File getWorkingFolder() {
        return workingFolder;
    }

    class DeployerLocalWorkspace extends LocalWorkspaceImpl {
        public DeployerLocalWorkspace(WorkspaceUser user, File location) {
            super(user, location);
        }

        @Override
        public LocalProjectImpl downloadProject(Project project) throws ProjectException {
            return super.downloadProject(project);
        }
    }
}
