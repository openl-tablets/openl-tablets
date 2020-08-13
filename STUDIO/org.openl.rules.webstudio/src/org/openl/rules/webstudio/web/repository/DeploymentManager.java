package org.openl.rules.webstudio.web.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.repository.deployment.DeploymentManifestBuilder;
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
    public RepositoryFactoryProxy repositoryFactoryProxy;

    public void addRepository(String repositoryConfigName) {
        deployers.add(repositoryConfigName);
    }

    public void removeRepository(String repositoryConfigName) {
        deployers.remove(repositoryConfigName);
        repositoryFactoryProxy.releaseRepository(repositoryConfigName);
    }

    Collection<String> getRepositoryConfigNames() {
        return deployers;
    }

    public DeployID deploy(ADeploymentProject project, String repositoryConfigName) throws ProjectException {
        if (!deployers.contains(repositoryConfigName)) {
            throw new IllegalArgumentException(String.format("No such repository '%s'", repositoryConfigName));
        }

        String userName = WebStudioUtils.getRulesUserSession().getUserName();

        @SuppressWarnings("rawtypes")
        Collection<ProjectDescriptor> projectDescriptors = project.getProjectDescriptors();

        try {
            Repository deployRepo = repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);
            StringBuilder sb = new StringBuilder(project.getName());
            ProjectVersion projectVersion = project.getVersion();
            boolean includeVersionInDeploymentName = repositoryFactoryProxy
                .isIncludeVersionInDeploymentName(repositoryConfigName);
            String deploymentsPath = repositoryFactoryProxy.getBasePath(repositoryConfigName);
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
            if (deployRepo.supports().folders()) {
                FolderRepository folderRepo = (FolderRepository) deployRepo;

                try (FileChangesToDeploy changes = new FileChangesToDeploy(projectDescriptors,
                    designRepository,
                        rulesPath,
                        deploymentPath,
                        userName)) {
                    FileData deploymentData = new FileData();
                    deploymentData.setName(deploymentName);
                    deploymentData.setAuthor(userName);
                    deploymentData.setComment(project.getFileData().getComment());
                    folderRepo.save(deploymentData, changes, ChangesetType.FULL);
                }
            } else {
                List<FileData> existingProjects = deployRepo.list(deploymentPath);
                List<FileData> projectsToDelete = findProjectsToDelete(existingProjects, projectDescriptors);
                for (FileData fileData : projectsToDelete) {
                    fileData.setAuthor(userName);
                    deployRepo.delete(fileData);
                }

                for (ProjectDescriptor<?> pd : projectDescriptors) {
                        String repositoryId = pd.getRepositoryId();
                        if (repositoryId == null) {
                            repositoryId = designRepository.getRepositories().get(0).getId();
                        }
                        Repository designRepo = designRepository.getRepository(repositoryId);
                    String version = pd.getProjectVersion().getVersionName();
                    String projectName = pd.getProjectName();

                    FileData dest = new FileData();
                    dest.setName(deploymentPath + projectName);
                    dest.setAuthor(userName);
                    dest.setComment(project.getFileData().getComment());

                    DeploymentManifestBuilder manifestBuilder = new DeploymentManifestBuilder()
                            .setBuiltBy(userName)
                            .setBuildNumber(pd.getProjectVersion().getRevision())
                            .setImplementationTitle(projectName);

                    if (designRepo.supports().folders()) {
                        if (designRepo.supports().branches()) {
                            manifestBuilder.setBranchName(((BranchRepository) designRepo).getBranch());
                        }
                        archiveAndSave((FolderRepository) designRepo,
                                rulesPath,
                                projectName,
                                version,
                                deployRepo,
                                dest,
                                manifestBuilder.build());
                    } else {
                        FileItem srcPrj = designRepo.readHistory(rulesPath + projectName, version);
                        includeManifestIntoArchiveAndSave(deployRepo, dest, srcPrj.getStream(), manifestBuilder.build());
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

    private void includeManifestIntoArchiveAndSave(Repository deployRepo, FileData dest, InputStream in, Manifest manifest) throws ProjectException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            RepositoryUtils.includeManifestAndRepackArchive(in, out, manifest);
            dest.setSize(out.size());
            deployRepo.save(dest, new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void archiveAndSave(FolderRepository designRepo,
                                String rulesPath,
                                String projectName,
                                String version,
                                Repository deployRepo,
                                FileData dest,
                                Manifest manifest) throws ProjectException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            RepositoryUtils.archive(designRepo, rulesPath, projectName, version, out, manifest);
            dest.setSize(out.size());
            deployRepo.save(dest, new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        }
    }

    private List<FileData> findProjectsToDelete(List<FileData> existingProjects,
            Collection<ProjectDescriptor> projectsToDeploy) {
        List<FileData> projectsToDelete = new ArrayList<>(existingProjects);
        // Filter out projects that will be replaced with a new version
        for (ProjectDescriptor projectToDeploy : projectsToDeploy) {
            for (Iterator<FileData> it = projectsToDelete.iterator(); it.hasNext();) {
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
        for (ProjectDescriptor pd : deploymentConfiguration.getProjectDescriptors()) {
            try {
                try {
                    String repositoryId = pd.getRepositoryId();
                    if (repositoryId == null) {
                        repositoryId = designRepository.getRepositories().get(0).getId();
                    }
                    AProject project = designRepository.getProject(repositoryId, pd.getProjectName(), pd.getProjectVersion());

                    AProjectArtefact artifact = project.getArtefact(DeployUtils.RULES_DEPLOY_XML);
                    if (artifact instanceof AProjectResource) {
                        AProjectResource resource = (AProjectResource) artifact;
                        try (InputStream content = resource.getContent()) {
                            RulesDeploy rulesDeploy = rulesDeploySerializer.deserialize(content);
                            String apiVersion = rulesDeploy.getVersion();
                            if (StringUtils.isNotBlank(apiVersion)) {
                                return apiVersion;
                            }
                        }
                    }
                } catch (ProjectException ignored) {
                }
            } catch (Throwable e) {
                log.error(
                    "Project loading from repository was failed! Project with name '{}' in deploy configuration '{}' has been skipped.",
                    pd.getProjectName(),
                    deploymentConfiguration.getName(),
                    e);
            }
        }

        return null;
    }

    public void setRepositoryFactoryProxy(RepositoryFactoryProxy repositoryFactoryProxy) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
    }

    public Repository getDeployRepository(String repositoryConfigName) throws RRepositoryException {
        return repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);
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
