package org.openl.rules.workspace.deploy.impl.jcr;

import org.openl.SmartProps;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.util.ZipUtil;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.openl.rules.workspace.lw.impl.LocalProjectImpl;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of <code>ProductionDeployer</code>  that uses <i>JCR</i> as production repository.
 */
public class JcrProductionDeployer implements ProductionDeployer {
    public static final String PROPNAME_ZIPFOLDER = "temp.zip.location";
    public static final String DEFAULT_ZIPFOLDER = "/tmp/rules-deployment/";
    public static final String WORKING_FOLDER = "work";
    static final String ZIP_FILE_NAME = "project.zip";
    public static final String ZIP_NODE_NAME = "deployment";
    /**
     * A folder inside of <code>userFolder</code>. A place to download deployed projects into before zipping and actual
     * deploying.
     */
    private final File workingFolder;
    /**
     * A folder to perform file operation in for given user. 
     */
    private final File userFolder;

    private final DeployerLocalWorkspace localWorkspace;
    /**
     * The user.
     */
    private final WorkspaceUser user;

    public JcrProductionDeployer(WorkspaceUser user, SmartProps props) throws DeploymentException {
        String location = props.getStr(PROPNAME_ZIPFOLDER, DEFAULT_ZIPFOLDER);
        userFolder = new File(location, user.getUserId());
        workingFolder = new File(userFolder, WORKING_FOLDER);

        ensureWorkingFolderExists();

        localWorkspace = new DeployerLocalWorkspace(user, workingFolder);
        this.user = user;
    }

    private void ensureWorkingFolderExists() throws DeploymentException {
        if (!FolderHelper.checkOrCreateFolder(workingFolder)) {
            throw new DeploymentException("can not create temp folder: {0}", null, workingFolder);
        }
    }

    /**
     * Deploys a collection of <code>Project</code>s to the production repository. Generates unique ID for the
     * deployment.
     *
     * @param projects projects to deploy
     * @return generated id for this deployment
     * @throws DeploymentException if any deployment error occures
     */
    public DeployID deploy(Collection<? extends Project> projects) throws DeploymentException {
        StringBuilder name = new StringBuilder();
        for (Project p : projects) {
            name.append(p.getName());
            if (p.getVersion() != null)
                name.append('-').append(p.getVersion().getVersionName());
            name.append('_');
        }
        name.append(System.currentTimeMillis());
        return deploy(new DeployID(name.toString()), projects);
    }

    /**
     * Deploys a collection of <code>Project</code>s to the production repository with given ID.
     * Overwrites deployment with given <i>id</i> if it already exists.
     *
     * @param projects projects to deploy
     * @return <code>id</code> parameter
     * @throws DeploymentException if any deployment error occures
     */

    public synchronized DeployID deploy(DeployID id, Collection<? extends Project> projects) throws DeploymentException {
        if (!FolderHelper.clearFolder(userFolder)) {
            throw new DeploymentException("could not delete folder; {0}", null, userFolder);
        }
        ensureWorkingFolderExists();

        downloadProjects(projects);

        try {
            ZipUtil.zipFolder(getWorkingFolder(), getZipFile());
        } catch (IOException e) {
            throw new DeploymentException("could not make projects zip archive", e);
        }

        remoteDeploy(id);

        return id;
    }

    private File getZipFile() {
        return new File(userFolder, ZIP_FILE_NAME);
    }

    private void remoteDeploy(DeployID id) throws DeploymentException {
        try {
            RRepository rRepository = ProductionRepositoryFactoryProxy.getRepositoryInstance();

            RProject rProject;
            RFile rFile = null;
            synchronized (rRepository) {
                if (rRepository.hasProject(id.getName())) {
                    rProject = rRepository.getProject(id.getName());
                    List<RFile> rFiles = rProject.getRootFolder().getFiles();
                    for (RFile file : rFiles) {
                        if (ZIP_NODE_NAME.equals(file.getName())) {
                            rFile = file;
                            break;
                        }
                    }
                } else {
                    rProject = rRepository.createProject(id.getName());
                }
            }

            if (rFile == null) {
                rFile = rProject.getRootFolder().createFile(ZIP_NODE_NAME);
            }
            
            try {
                FileInputStream fis = new FileInputStream(getZipFile());
                rFile.setContent(fis);
                fis.close();
            } catch (IOException e) {
                throw new DeploymentException("IO exception", e);
            }
            rProject.commit(user);

        } catch (RRepositoryException e) {
            throw new DeploymentException("failed to get repository", e);
        }
    }

    protected void downloadProjects(Collection<? extends Project> projects) throws DeploymentException {
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

    static class DeployerLocalWorkspace extends LocalWorkspaceImpl {
        public DeployerLocalWorkspace(WorkspaceUser user, File location) {
            super(user, location);
        }

        @Override
        public LocalProjectImpl downloadProject(Project project) throws ProjectException {
            return super.downloadProject(project);
        }
    }
}
