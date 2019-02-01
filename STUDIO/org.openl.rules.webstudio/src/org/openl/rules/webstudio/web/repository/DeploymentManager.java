package org.openl.rules.webstudio.web.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeployUtils;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Deployment manager
 *
 * @author Andrey Naumenko
 */
public class DeploymentManager implements InitializingBean {
    private final Logger log = LoggerFactory.getLogger(DeploymentManager.class);

    private String[] initialProductionRepositoryConfigNames;
    private DesignTimeRepository designRepository;
    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    private Set<String> deployers = new HashSet<>();

    public void addRepository(String repositoryConfigName) {
        deployers.add(repositoryConfigName);
    }

    public void removeRepository(String repositoryConfigName) {
        deployers.remove(repositoryConfigName);
        repositoryFactoryProxy.releaseRepository(repositoryConfigName);
    }

    public Collection<String> getRepositoryConfigNames() {
        return deployers;
    }

    public DeployID deploy(ADeploymentProject project, String repositoryConfigName) throws ProjectException {
        if (!deployers.contains(repositoryConfigName)) {
            throw new IllegalArgumentException("No such repository '" + repositoryConfigName + "'");
        }

        String userName = WebStudioUtils.getRulesUserSession(FacesUtils.getSession()).getUserName();

        @SuppressWarnings("rawtypes")
        Collection<ProjectDescriptor> projectDescriptors = project.getProjectDescriptors();

        try {
            Repository deployRepo = repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);
            StringBuilder sb = new StringBuilder(project.getName());
            ProjectVersion projectVersion = project.getVersion();
            boolean includeVersionInDeploymentName = repositoryFactoryProxy.isIncludeVersionInDeploymentName( repositoryConfigName);
            String deploymentsPath = repositoryFactoryProxy.getDeploymentsPath(repositoryConfigName);
            if (projectVersion != null) {
                if (includeVersionInDeploymentName) {
                    int version = DeployUtils.getNextDeploymentVersion(deployRepo, project.getName(), deploymentsPath);
                    sb.append(DeployUtils.SEPARATOR).append(version);
                } else {
                    String apiVersion = getApiVersion(project);
                    if (apiVersion != null) {
                        sb.append(DeployUtils.API_VERSION_SEPARATOR).append(apiVersion);
                    }
                }
            }
            DeployID id = new DeployID(sb.toString());

            String deploymentName = deploymentsPath + id.getName();
            String deploymentPath = deploymentName + "/";

            String rulesPath = designRepository.getRulesLocation();
            if (deployRepo instanceof FolderRepository) {
                FolderRepository folderRepo = (FolderRepository) deployRepo;

                Repository designRepo = designRepository.getRepository();
                try (FileChangesToDeploy changes = new FileChangesToDeploy(projectDescriptors,
                        designRepo,
                        rulesPath,
                        deploymentPath)) {
                    FileData deploymentData = new FileData();
                    deploymentData.setName(deploymentName);
                    deploymentData.setAuthor(userName);
                    deploymentData.setComment(project.getFileData().getComment());
                    folderRepo.save(deploymentData, changes);
                }
            } else {
                List<FileData> existingProjects = deployRepo.list(deploymentPath);
                List<FileData> projectsToDelete = findProjectsToDelete(existingProjects, projectDescriptors);
                for (FileData fileData : projectsToDelete) {
                    fileData.setAuthor(userName);
                    deployRepo.delete(fileData);
                }

                Repository designRepo = designRepository.getRepository();
                for (ProjectDescriptor<?> pd : projectDescriptors) {
                    InputStream stream = null;
                    try {
                        String version = pd.getProjectVersion().getVersionName();
                        String projectName = pd.getProjectName();

                        FileData dest = new FileData();
                        dest.setName(deploymentPath + projectName);
                        dest.setAuthor(userName);
                        dest.setComment(project.getFileData().getComment());

                        if (designRepo instanceof FolderRepository) {
                            String projectPath = rulesPath + projectName + "/";
                            archiveAndSave((FolderRepository) designRepo, projectPath, version, deployRepo, dest);
                        } else {
                            FileItem srcPrj = designRepo.readHistory(rulesPath + projectName, version);
                            stream = srcPrj.getStream();
                            dest.setSize(srcPrj.getData().getSize());
                            deployRepo.save(dest, stream);
                        }
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
            }

            // TODO: Some analogue of notifyChanges() possibly will be needed
            // rRepository.notifyChanges();
            return id;
        } catch (Exception e) {
            throw new DeploymentException("Failed to deploy: " + e.getMessage(), e);
        }
    }

    private void archiveAndSave(FolderRepository designRepo, String projectPath, String version, Repository deployRepo, FileData dest) throws ProjectException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(out);

            List<FileData> files = designRepo.listFiles(projectPath, version);

            for (FileData file : files) {
                String internalPath = file.getName().substring(projectPath.length());
                zipOutputStream.putNextEntry(new ZipEntry(internalPath));

                FileItem fileItem = designRepo.readHistory(file.getName(), file.getVersion());
                try (InputStream content = fileItem.getStream()) {
                    IOUtils.copy(content, zipOutputStream);
                }

                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();

            dest.setSize(out.size());

            deployRepo.save(dest, new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    private List<FileData> findProjectsToDelete(List<FileData> existingProjects, Collection<ProjectDescriptor> projectsToDeploy) {
        List<FileData> projectsToDelete = new ArrayList<>(existingProjects);
        // Filter out projects that will be replaced with a new version
        for (ProjectDescriptor projectToDeploy : projectsToDeploy) {
            for (Iterator<FileData> it = projectsToDelete.iterator(); it.hasNext(); ) {
                String folderPath = it.next().getName();
                String projectName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
                if (projectName.equals(projectToDeploy.getProjectName())) {
                    // This project will be replaced with a new version. No need to delete it
                    it.remove();
                    break;
                }
            }
        }
        return projectsToDelete;
    }

    private String getApiVersion(ADeploymentProject deploymentConfiguration) {
        Repository designRepo = designRepository.getRepository();

        for (ProjectDescriptor pd : deploymentConfiguration.getProjectDescriptors()) {
            try {
                InputStream content = null;
                try {
                    String projectVersion = pd.getProjectVersion().getVersionName();
                    String projectName = pd.getProjectName();
                    AProject project = new AProject(designRepo, designRepository.getRulesLocation() + projectName, projectVersion);

                    AProjectArtefact artifact = project.getArtefact(DeployUtils.RULES_DEPLOY_XML);
                    if (artifact instanceof AProjectResource) {
                        AProjectResource resource = (AProjectResource) artifact;
                        content = resource.getContent();
                        RulesDeploy rulesDeploy = rulesDeploySerializer.deserialize(content);
                        String apiVersion = rulesDeploy.getVersion();
                        if (StringUtils.isNotBlank(apiVersion)) {
                            return apiVersion;
                        }
                    }
                } catch (ProjectException ignored) {
                } finally {
                    if (content != null) {
                        try {
                            content.close();
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            } catch (Throwable e) {
                log.error(
                        "Project loading from repository was failed! Project with name \"{}\" in deploy configuration \"{}\" was skipped!",
                        pd.getProjectName(),
                        deploymentConfiguration.getName(),
                        e);
            }
        }

        return null;
    }

    private ProductionRepositoryFactoryProxy repositoryFactoryProxy;

    public void setRepositoryFactoryProxy(ProductionRepositoryFactoryProxy repositoryFactoryProxy) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
    }

    public void setInitialProductionRepositoryConfigNames(String[] initialProductionRepositoryConfigNames) {
        this.initialProductionRepositoryConfigNames = initialProductionRepositoryConfigNames;
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
    }

    @Override
    public void afterPropertiesSet() {
        if (initialProductionRepositoryConfigNames != null) {
            for (String repositoryConfigName : initialProductionRepositoryConfigNames) {
                addRepository(repositoryConfigName);
            }
        }
    }
}
