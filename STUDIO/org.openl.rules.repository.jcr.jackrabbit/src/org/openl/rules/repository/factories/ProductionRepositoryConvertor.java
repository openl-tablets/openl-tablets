package org.openl.rules.repository.factories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.ValueType;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonUserImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RProductionDeployment;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrEntityAPI;
import org.openl.rules.repository.jcr.JcrFileAPI;
import org.openl.rules.repository.jcr.JcrFolderAPI;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.jcr.JcrProductionRepository;
import org.openl.rules.repository.jcr.NodeUtil;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//FIXME refactor to use AProjectArtefacts
public class ProductionRepositoryConvertor {
    private final Logger log = LoggerFactory.getLogger(ProductionRepositoryConvertor.class);

    public static final CommonVersion from = new CommonVersionImpl(7);
    public static final CommonVersion to = new CommonVersionImpl(4);
    private final CommonUser system = new CommonUserImpl("system");

    private final File repHome;

    private TransientRepository repo;
    private Session target;

    public ProductionRepositoryConvertor(File tempRepositoryHome) {
        this.repHome = tempRepositoryHome;
    }

    protected Node checkPath(String aPath) throws RepositoryException {
        Node node = target.getRootNode();
        String[] paths = aPath.split("/");
        for (String path : paths) {
            if (path.length() == 0) {
                continue; // first element (root folder) or illegal path
            }

            if (node.hasNode(path)) {
                // go deeper
                node = node.getNode(path);
            } else {
                // create new
                node = node.addNode(path);
            }
        }

        return node;
    }

    public void convert(RProductionRepository repository) throws Exception {
        try {
            createTempRepository();
            copyDeployments(repository);
        } finally {
            if (target != null) {
                target.logout();
            }
        }
    }

    private void createTempRepository() throws Exception {
        String repConf = "/jackrabbit-repository.xml";

        // obtain real path to repository configuration file
        InputStream input = this.getClass().getResourceAsStream(repConf);
        File tempRepositorySettings = File.createTempFile("jackrabbit-repository", ".xml");
        // It could be cleaned-up on exit
        tempRepositorySettings.deleteOnExit();

        OutputStream tempRepositorySettingsStream = new FileOutputStream(tempRepositorySettings);
        IOUtils.copyAndClose(input, tempRepositorySettingsStream);

        repo = new TransientRepository(tempRepositorySettings, repHome);
        // TODO: schema
        target = createSession("user", "pass");
        NodeTypeManager ntm = target.getWorkspace().getNodeTypeManager();
        NodeTypeManagerImpl ntmi = (NodeTypeManagerImpl) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream("/org/openl/rules/repository/openl_nodetypes.xml");
                ntmi.registerNodeTypes(is, JackrabbitNodeTypeManager.TEXT_XML, true);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to init NodeTypes: " + e.getMessage(), e);
        }

        checkPath(JcrProductionRepository.DEPLOY_ROOT);
        target.save();
    }

    protected Session createSession(String user, String pass) throws RepositoryException {
        char[] password = pass.toCharArray();
        SimpleCredentials sc = new SimpleCredentials(user, password);
        return repo.login(sc);
    }

    private void copyDeployments(RProductionRepository repository) {
        try {
            for (String deploymentName : repository.getDeploymentNames()) {
                copyDeployment(repository.getDeployment(deploymentName), repository);
            }
        } catch (RRepositoryException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void copyDeployment(RProductionDeployment deployment, RProductionRepository repository) {
        try {
            Node node = NodeUtil.createNode(checkPath(JcrProductionRepository.DEPLOY_ROOT), deployment.getName(),
                    JcrNT.NT_APROJECT, true);
            checkPath(JcrProductionRepository.DEPLOY_ROOT).save();
            JcrFolderAPI deploymentFolder = new JcrFolderAPI(node, repository.getTransactionManager(),
                    new ArtefactPathImpl(new String[] { deployment.getName() }));
            copyEntity(deployment, deploymentFolder);
            for (RProject project : deployment.getProjects()) {
                copyProject(project, deploymentFolder);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void copyProject(RProject project, JcrFolderAPI deploymentFolder) {
        try {
            Node node = NodeUtil.createNode(deploymentFolder.node(), project.getName(), JcrNT.NT_APROJECT, true);
            deploymentFolder.node().save();
            JcrFolderAPI jcrProject = new JcrFolderAPI(node, deploymentFolder.getTransactionManager(),
                    new ArtefactPathImpl(new String[] { project.getName() }));
            copyFolder(project, jcrProject);
            CommonUser user = system;
            RVersion version = project.getActiveVersion();
            jcrProject.commit(user,version.getRevision());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void copyFolder(REntity wasfolder, JcrFolderAPI newFolder) throws Exception {
        copyEntity(wasfolder, newFolder);
        RFolder folder;
        if (wasfolder instanceof RProject) {
            folder = ((RProject) wasfolder).getRootFolder();
        } else {
            folder = (RFolder) wasfolder;
        }

        Set<String> artefactNames = new HashSet<String>();
        for (RFile file : folder.getFiles()) {
            artefactNames.add(file.getName());
        }
        for (RFolder dir : folder.getFolders()) {
            artefactNames.add(dir.getName());
        }

        // remove absent
        for (ArtefactAPI artefact : newFolder.getArtefacts()) {
            String name = artefact.getName();

            if (!artefactNames.contains(name)) {
                // was deleted
                artefact.delete(system);
            }
        }

        for (RFolder child : folder.getFolders()) {
            String name = child.getName();
            if (newFolder.hasArtefact(name) && newFolder.getArtefact(name).isFolder()) {
                copyFolder(child, (JcrFolderAPI) newFolder.getArtefact(name));
            } else {
                if (newFolder.hasArtefact(name)) {
                    // another type;
                    newFolder.getArtefact(name).delete(system);
                }
                copyFolder(child, newFolder.addFolder(name));
            }
        }
        for (RFile child : folder.getFiles()) {
            String name = child.getName();
            if (newFolder.hasArtefact(name) && !newFolder.getArtefact(name).isFolder()) {
                copyFile(child, (JcrFileAPI) newFolder.getArtefact(name));
            } else {
                if (newFolder.hasArtefact(name)) {
                    // another type;
                    newFolder.getArtefact(name).delete(system);
                }
                copyFile(child, newFolder.addResource(name, child.getContent()));
            }
        }
    }

    private void copyFile(RFile file, JcrFileAPI newFile) throws Exception {
        copyEntity(file, newFile);
        newFile.setContent(file.getContent());
    }

    private void copyEntity(REntity entity, JcrEntityAPI newEntity) throws Exception {
        try {
            newEntity.setProps(entity.getProps());
        } catch (PropertyException e1) {
            if (log.isErrorEnabled()) {
                log.error(e1.getMessage(), e1);
            }
        }
        try {
            newEntity.removeAllProperties();
            Date effectiveDate = entity.getEffectiveDate();
            if (effectiveDate != null) {
                newEntity.addProperty(org.openl.rules.repository.api.ArtefactProperties.PROP_EFFECTIVE_DATE,
                        ValueType.DATE, effectiveDate);
            }
            Date expirationDate = entity.getExpirationDate();
            if (expirationDate != null) {
                newEntity.addProperty(org.openl.rules.repository.api.ArtefactProperties.PROP_EXPIRATION_DATE,
                        ValueType.DATE, expirationDate);
            }
            String lob = entity.getLineOfBusiness();
            if (lob != null) {
                newEntity.addProperty(org.openl.rules.repository.api.ArtefactProperties.PROP_LINE_OF_BUSINESS,
                        ValueType.STRING, lob);
            }
        } catch (PropertyException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
