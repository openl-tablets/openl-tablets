package org.openl.rules.runtime.deployer;

import com.exigen.common.repository.axis.DeployerSOAPStub;
import com.exigen.common.repository.axis.Deployer_ServiceLocator;
import com.exigen.common.repository.deployment.Deployer;
import com.exigen.common.repository.deployment.DeploymentStatus;
import com.exigen.common.repository.deployment.axis.DeployerSOAPClient;

import org.openl.rules.workspace.deploy.DeploymentException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.ProductionDeployer;

import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.rpc.ServiceException;

/**
 * Deploys projects into Runtime Repository.
 * 
 * @author Andrey Naumenko
 */
public class RuntimeRepositoryDeployer implements ProductionDeployer {
    private final static Log log = LogFactory.getLog(RuntimeRepositoryDeployer.class);
    private static final int BUFFER_SIZE = 4096;
    private Deployer deployer;
    private String deployerUrl;

    public RuntimeRepositoryDeployer(String deployerUrl) throws ServiceException {
        this.deployerUrl = deployerUrl;
        deployer = createDeployer();
    }

    /**
     * Deploys a <code>Project</code>s into the Runtime Repository.
     * 
     * @param project project to deploy
     */
    public void deploy(Project project) {
    }

    /**
     * DOCUMENT ME!
     * 
     * @param projects
     */
    public void deploy(Collection<Project> projects) {
        for (Project project : projects) {
            deploy(project);
        }
    }

    private Deployer createDeployer() throws ServiceException {
        DeployerSOAPStub client = null;
        Deployer_ServiceLocator deployerServiceLocator = new Deployer_ServiceLocator();
        deployerServiceLocator.setdeployerSOAPEndpointAddress(deployerUrl);
        client = (DeployerSOAPStub) deployerServiceLocator.getdeployerSOAP();
        client.setTimeout(0);
        return new DeployerSOAPClient(client);
    }

    /**
     * Copy the contents of the given InputStream to the given OutputStream.
     * Closes input stream when done.
     * 
     * @param in the stream to copy from
     * @param out the stream to copy to
     * 
     * @return the number of bytes copied
     * 
     * @throws IOException in case of I/O errors
     */
    private static int copy(InputStream in, OutputStream out) throws IOException {
        Assert.notNull(in, "No InputStream specified");
        Assert.notNull(out, "No OutputStream specified");
        try {
            int byteCount = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return byteCount;
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                log.warn("Could not close InputStream", ex);
            }
        }
    }

    private static void zipProject(String path, ProjectFolder projectFolder, ZipOutputStream out) throws IOException,
            ProjectException {
        for (Iterator<? extends ProjectArtefact> i = projectFolder.getArtefacts().iterator(); i.hasNext();) {
            ProjectArtefact artefact = i.next();

            if (artefact instanceof ProjectResource) {
                ProjectResource resource = (ProjectResource) artefact;
                ZipEntry e = new ZipEntry(path + artefact.getName());

                out.putNextEntry(e);
                copy(resource.getContent(), out);
            } else if (artefact instanceof ProjectFolder) {
                ProjectFolder folder = (ProjectFolder) artefact;
                String folderPath = path + artefact.getName() + "/";
                ZipEntry e = new ZipEntry(folderPath);
                out.putNextEntry(e);
                zipProject(folderPath, folder, out);
            }
        }
    }

    public DeployID deploy(Collection<? extends Project> projects)
            throws org.openl.rules.workspace.deploy.DeploymentException {
        return deploy(null, projects);
    }

    public DeployID deploy(DeployID id, Collection<? extends Project> projects) throws DeploymentException {
        for (Project project : projects) {

            ZipOutputStream out = null;
            File file = null;

            try {
                file = File.createTempFile("project", "archive");
                out = new ZipOutputStream(new FileOutputStream(file));
                ZipEntry e = new ZipEntry("module.xml");
                out.putNextEntry(e);
                String module = "<?xml version=\"1.0\" encoding=\"ASCII\"?>"
                        + "<module:Descriptor xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:module=\"http://exigengroup.com/common/repository/module\" id=\""
                        + project.getName() + "\" version=\"1.0\">" + "<packaging sourceDirectory=\"src\"/>"
                        + "</module:Descriptor>";

                out.write(module.getBytes());
                zipProject("", project, out);
            } catch (ProjectException e) {
                log.error("Error deploying project: ", e);
            } catch (IOException e) {
                log.error("Error deploying project: ", e);
            } finally {
                IOUtils.closeQuietly(out);
            }

            try {
                DeploymentStatus status = deployer.deployModules(new File[] { file }, true);
                log.info("File " + file + " was deployed into runtime repository with status: " + status);
            } catch (com.exigen.common.repository.deployment.DeploymentException e) {
                log.error("Failed to deploy project:", e);
                throw new DeploymentException("Failed to deploy project.", e);
            } finally {
                if (file != null) {
                    file.delete();
                }
            }
        }
        return null;
    }
}
