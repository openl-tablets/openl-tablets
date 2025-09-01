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
import java.util.Objects;
import java.util.Set;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
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
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.deployment.DeploymentManifestBuilder;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * Deployment manager
 *
 * @author Andrey Naumenko
 */
public class DeploymentManager implements InitializingBean {
    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";
    private static final String API_VERSION_SEPARATOR = "_V";
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentManager.class);

    private String[] initialProductionRepositoryConfigNames;
    private DesignTimeRepository designRepository;
    private IRulesDeploySerializer rulesDeploySerializer;
    private PropertyResolver propertyResolver;

    private final Set<String> deployers = new HashSet<>();
    public RepositoryFactoryProxy repositoryFactoryProxy;

    public void addRepository(String repositoryConfigName) {
        deployers.add(repositoryConfigName);
    }

    public Collection<String> getRepositoryConfigNames() {
        return deployers;
    }

    public String validateOnMainBranch(List<ADeploymentProject> projects, String repositoryConfigName) {
        if (projects == null || projects.isEmpty() || repositoryConfigName == null) {
            return null;
        }
        if (!new RepositoryConfiguration(repositoryConfigName, propertyResolver).getSettings().isMainBranchOnly()) {
            return null;
        }

        return projects.stream()
                .filter(Objects::nonNull)
                .flatMap(x -> x.getProjectDescriptors().stream())
                .map(x -> {
                    var repo = designRepository.getRepository(x.repositoryId());
                    if (repo == null) {
                        return null;
                    }
                    if (!repo.supports().branches()) {
                        return null;
                    }
                    var repoMainBranch = ((BranchRepository) repo).getBranch();
                    if (repoMainBranch.equals(x.branch())) {
                        return null;
                    }
                    return repoMainBranch;
                })
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    public DeployID deploy(ADeploymentProject project, String repositoryConfigName, String comment) throws ProjectException {

        CommonUser user = WebStudioUtils.getRulesUserSession().getUserWorkspace().getUser();
        var request = DeploymentRequest.builder()
                .name(project.getName())
                .productionRepositoryId(repositoryConfigName)
                .projectDescriptors(project.getProjectDescriptors())
                .currentUser(user)
                .comment(comment);
        return deploy(request.build());
    }

    public DeployID deploy(DeploymentRequest request) throws DeploymentException {
        if (!deployers.contains(request.productionRepositoryId())) {
            throw new IllegalArgumentException(String.format("Repository '%s' is not found.", request.productionRepositoryId()));
        }
        try {
            StringBuilder sb = new StringBuilder(request.name());
            String apiVersion = getApiVersion(request.name(), request.projectDescriptors());
            if (apiVersion != null) {
                sb.append(API_VERSION_SEPARATOR).append(apiVersion);
            }
            DeployID id = new DeployID(sb.toString());

            Repository deployRepo = repositoryFactoryProxy.getRepositoryInstance(request.productionRepositoryId());
            String deploymentsPath = repositoryFactoryProxy.getBasePath(request.productionRepositoryId());

            String deploymentName = deploymentsPath + id.getName();
            String deploymentPath = deploymentName + "/";

            String rulesPath = designRepository.getRulesLocation();
            if (deployRepo.supports().folders()) {
                try (FileChangesToDeploy changes = new FileChangesToDeploy(request.projectDescriptors(),
                        designRepository,
                        rulesPath,
                        deploymentPath,
                        request.currentUser().getUserName())) {
                    FileData deploymentData = new FileData();
                    deploymentData.setName(deploymentName);
                    deploymentData.setAuthor(request.currentUser().getUserInfo());
                    deploymentData.setComment(request.comment());
                    deployRepo.save(deploymentData, changes, ChangesetType.FULL);
                }
            } else {
                List<FileData> existingProjects = deployRepo.list(deploymentPath);
                List<FileData> projectsToDelete = findProjectsToDelete(existingProjects, request.projectDescriptors());
                for (FileData fileData : projectsToDelete) {
                    fileData.setAuthor(request.currentUser().getUserInfo());
                    deployRepo.delete(fileData);
                }

                for (ProjectDescriptor pd : request.projectDescriptors()) {
                    String repositoryId = pd.repositoryId();
                    if (repositoryId == null) {
                        repositoryId = designRepository.getRepositories().getFirst().getId();
                    }
                    Repository designRepo = designRepository.getRepository(repositoryId);
                    String version = pd.projectVersion().getVersionName();
                    String projectName = pd.projectName();
                    String projectPath = pd.path();
                    String branch = pd.branch();

                    FileData dest = new FileData();
                    dest.setName(deploymentPath + projectName);
                    dest.setAuthor(request.currentUser().getUserInfo());
                    dest.setComment(request.comment());

                    final FileData historyData = designRepo.checkHistory(rulesPath + projectName, version);
                    DeploymentManifestBuilder manifestBuilder = new DeploymentManifestBuilder()
                            .setBuiltBy(request.currentUser().getUserName())
                            .setBuildNumber(pd.projectVersion().getRevision())
                            .setImplementationTitle(projectName)
                            .setImplementationVersion(RepositoryUtils.buildProjectVersion(historyData));
                    if (pd.branch() != null) {
                        manifestBuilder.setBuildBranch(pd.branch());
                    }

                    if (designRepo.supports().folders()) {
                        String technicalName = projectName;
                        AProject designProject = designRepository
                                .getProjectByPath(repositoryId, branch, projectPath, version);
                        if (designProject != null) {
                            technicalName = designProject.getName();
                        }
                        archiveAndSave(designRepo,
                                rulesPath,
                                technicalName,
                                version,
                                deployRepo,
                                dest,
                                manifestBuilder.build());
                    } else {
                        FileItem srcPrj = designRepo.readHistory(rulesPath + projectName, version);
                        includeManifestIntoArchiveAndSave(deployRepo,
                                dest,
                                srcPrj.getStream(),
                                manifestBuilder.build());
                    }
                }
            }
            return id;
        } catch (Exception e) {
            throw new DeploymentException("Failed to deploy: " + e.getMessage(), e);
        }
    }

    public String validateOnMainBranch(DeploymentRequest request) {
        var projectDescriptors = request.projectDescriptors();
        if (projectDescriptors == null || projectDescriptors.isEmpty() || request.productionRepositoryId() == null) {
            return null;
        }
        if (!new RepositoryConfiguration(request.productionRepositoryId(), propertyResolver).getSettings().isMainBranchOnly()) {
            return null;
        }

        return projectDescriptors.stream()
                .filter(Objects::nonNull)
                .map(x -> {
                    var repo = designRepository.getRepository(x.repositoryId());
                    if (repo == null) {
                        return null;
                    }
                    if (!repo.supports().branches()) {
                        return null;
                    }
                    var repoMainBranch = ((BranchRepository) repo).getBranch();
                    if (repoMainBranch.equals(x.branch())) {
                        return null;
                    }
                    return repoMainBranch;
                })
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private void includeManifestIntoArchiveAndSave(Repository deployRepo,
                                                   FileData dest,
                                                   InputStream in,
                                                   Manifest manifest) throws ProjectException {
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

    private void archiveAndSave(Repository designRepo,
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
            for (Iterator<FileData> it = projectsToDelete.iterator(); it.hasNext(); ) {
                String folderPath = it.next().getName();
                String projectName = folderPath.substring(folderPath.lastIndexOf('/') + 1);
                if (projectName.equals(projectToDeploy.projectName())) {
                    // This project will be replaced with a new version. No need to delete it
                    it.remove();
                    break;
                }
            }
        }
        return projectsToDelete;
    }

    private String getApiVersion(String deploymentName, Collection<ProjectDescriptor> projectDescriptors) {
        for (var pd : projectDescriptors) {
            try {
                try {
                    String repositoryId = pd.repositoryId();
                    if (repositoryId == null) {
                        repositoryId = designRepository.getRepositories().getFirst().getId();
                    }
                    String branch = pd.branch();
                    String projectPath = pd.path();
                    AProject project;
                    if (projectPath != null) {
                        project = designRepository.getProjectByPath(repositoryId,
                                branch,
                                projectPath,
                                pd.projectVersion().getVersionName());
                    } else {
                        project = designRepository
                                .getProject(repositoryId, pd.projectName(), pd.projectVersion());
                    }

                    AProjectArtefact artifact = project.getArtefact(RULES_DEPLOY_XML);
                    if (artifact instanceof AProjectResource resource) {
                        try (InputStream content = resource.getContent()) {
                            RulesDeploy rulesDeploy = getRulesDeploySerializer().deserialize(content);
                            String apiVersion = rulesDeploy.getVersion();
                            if (StringUtils.isNotBlank(apiVersion)) {
                                return apiVersion;
                            }
                        }
                    }
                } catch (ProjectException ignored) {
                }
            } catch (Exception e) {
                LOG.error(
                        "Project loading from repository was failed! " + "Project with name '{}' in deploy configuration '{}' has been skipped.",
                        pd.projectName(),
                        deploymentName,
                        e);
            }
        }

        return null;
    }

    private IRulesDeploySerializer getRulesDeploySerializer() {
        if (rulesDeploySerializer == null) {
            rulesDeploySerializer = new XmlRulesDeploySerializer();
        }
        return rulesDeploySerializer;
    }

    public void setRepositoryFactoryProxy(RepositoryFactoryProxy repositoryFactoryProxy) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
    }

    public Repository getDeployRepository(String repositoryConfigName) {
        return repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);
    }

    public void setInitialProductionRepositoryConfigNames(String[] initialProductionRepositoryConfigNames) {
        this.initialProductionRepositoryConfigNames = initialProductionRepositoryConfigNames;
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
    }

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
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
