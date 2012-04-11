package org.openl.rules.workspace.production.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RProductionDeployment;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.lw.impl.FolderHelper;

/**
 * This class can extract rules projects deployed into production JCR based
 * environment to a specified location on file system. Also it is a good place
 * for higher level utility methods on top of production repository API.
 */
public class JcrRulesClient {
    public void addListener(RDeploymentListener l) throws RRepositoryException {
        ProductionRepositoryFactoryProxy.getRepositoryInstance().addListener(l);
    }

    private void download(FolderAPI folder, File location) throws RRepositoryException, IOException {
        location.mkdirs();
        for (ArtefactAPI artefact : folder.getArtefacts()) {
            if(artefact.isFolder()){
                download((FolderAPI)artefact, new File(location, artefact.getName()));
            }else{
                try {
                    ResourceAPI resource = (ResourceAPI) artefact;
                    FileOutputStream os = new FileOutputStream(new File(location, resource.getName()));
                    IOUtils.copy(resource.getContent(), os);
                    IOUtils.closeQuietly(os);
                } catch (ProjectException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Unpacks deployed project with given deploy id to <code>destFolder</code>.
     * The methods uses <code>RRepository</code> instance provided by
     * <code>ProductionRepositoryFactoryProxy</code> factory as production
     * repository.
     *
     * @param deployID identifier of deployed project
     * @param destFolder the folder to unpack the project to.
     *
     * @throws Exception if an error occurres
     */
    public void fetchDeployment(DeployID deployID, File destFolder) throws Exception {
        destFolder.mkdirs();
        FolderHelper.clearFolder(destFolder);

        FolderAPI rDeployment = ProductionRepositoryFactoryProxy.getRepositoryInstance().getDeploymentProject(
                deployID.getName());

        //FIXME
        Collection<? extends ArtefactAPI> projects = rDeployment.getArtefacts();
        for (ArtefactAPI project : projects) {
            if(project.isFolder()){
            File projectFolder = new File(destFolder, project.getName());
            projectFolder.mkdirs();

            download((FolderAPI)project, projectFolder);
            }
        }
    }

    /**
     * Returns names of all existing deployments in production repository.
     *
     * @return collection of names
     * @throws RRepositoryException on repository error
     */
    public Collection<String> getDeploymentNames() throws RRepositoryException {
        return ProductionRepositoryFactoryProxy.getRepositoryInstance().getDeploymentProjectNames();
    }

    public void release() throws RRepositoryException {
        ProductionRepositoryFactoryProxy.release();
    }

    public void removeListener(RDeploymentListener l) throws RRepositoryException {
        ProductionRepositoryFactoryProxy.getRepositoryInstance().removeListener(l);
    }

}
