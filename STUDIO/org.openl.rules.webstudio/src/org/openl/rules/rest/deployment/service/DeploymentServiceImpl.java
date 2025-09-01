package org.openl.rules.rest.deployment.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.rest.model.ProjectIdModel;
import org.openl.rules.rest.project.ProjectStateValidator;
import org.openl.rules.rest.service.ProjectDependencyResolver;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentRequest;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;

public class DeploymentServiceImpl implements DeploymentService {

    private static final String SEPARATOR = "#";

    private final ProjectDependencyResolver projectDependencyResolver;
    private final SecureDeploymentRepositoryService deploymentRepositoryService;
    private final DeploymentManager deploymentManager;
    private final ObjectProvider<UserWorkspace> userWorkspaceProvider;
    private final ProjectStateValidator projectStateValidator;
    private final AclProjectsHelper aclProjectsHelper;

    public DeploymentServiceImpl(ProjectDependencyResolver projectDependencyResolver,
                                 SecureDeploymentRepositoryService deploymentRepositoryService,
                                 DeploymentManager deploymentManager,
                                 ObjectProvider<UserWorkspace> userWorkspaceProvider,
                                 ProjectStateValidator projectStateValidator,
                                 AclProjectsHelper aclProjectsHelper) {
        this.projectDependencyResolver = projectDependencyResolver;
        this.deploymentRepositoryService = deploymentRepositoryService;
        this.deploymentManager = deploymentManager;
        this.userWorkspaceProvider = userWorkspaceProvider;
        this.projectStateValidator = projectStateValidator;
        this.aclProjectsHelper = aclProjectsHelper;
    }

    @Override
    public List<Deployment> getDeployments(DeploymentCriteriaQuery query) {
        Stream<RepositoryConfiguration> repoConfigsStream;
        if (StringUtils.isNotBlank(query.repository())) {
            repoConfigsStream = deploymentRepositoryService.getRepository(query.repository()).stream();
        } else {
            repoConfigsStream = deploymentRepositoryService.getRepositories().stream();
        }
        return repoConfigsStream.flatMap(config -> {
                    try {
                        return listDeployments(config);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(query.getFilter())
                .sorted(RepositoryUtils.ARTEFACT_COMPARATOR)
                .toList();
    }

    private Stream<Deployment> listDeployments(RepositoryConfiguration config) throws IOException {
        var repository = deploymentManager.getDeployRepository(config.getId());
        var basePath = deploymentManager.repositoryFactoryProxy.getBasePath(config.getId());

        Map<String, Deployment> latestDeployments = new HashMap<>();
        Map<String, Integer> versionsList = new HashMap<>();

        Collection<FileData> fileDatas;
        if (repository.supports().folders()) {
            // All deployments
            fileDatas = repository.listFolders(basePath);
        } else {
            // Projects inside all deployments
            fileDatas = repository.list(basePath);
        }
        for (FileData fileData : fileDatas) {
            String deploymentFolderName = fileData.getName().substring(basePath.length()).split("/")[0];
            int separatorPosition = deploymentFolderName.lastIndexOf(SEPARATOR);

            String deploymentName = deploymentFolderName;
            int version = 0;
            CommonVersionImpl commonVersion;
            if (separatorPosition >= 0) {
                deploymentName = deploymentFolderName.substring(0, separatorPosition);
                version = Integer.parseInt(deploymentFolderName.substring(separatorPosition + 1));
                commonVersion = new CommonVersionImpl(version);
            } else {
                commonVersion = new CommonVersionImpl(fileData.getVersion());
            }
            Integer previous = versionsList.put(deploymentName, version);
            if (previous != null && previous > version) {
                // rollback
                versionsList.put(deploymentName, previous);
            } else {
                // put the latest deployment
                String folderPath = basePath + deploymentFolderName;
                boolean folderStructure;
                if (repository.supports().folders()) {
                    folderStructure = !repository.listFolders(folderPath + "/").isEmpty();
                } else {
                    folderStructure = false;
                }
                Deployment deployment = new Deployment(repository,
                        folderPath,
                        deploymentName,
                        commonVersion,
                        folderStructure);
                latestDeployments.put(deploymentName, deployment);
            }
        }

        return latestDeployments.values().stream();
    }

    @Override
    public void deploy(ProjectIdModel deploymentId, RulesProject project, String comment) throws ProjectException {
        if (!projectStateValidator.canDeploy(project)) {
            if (project.isDeleted()) {
                throw new ConflictException("project.deploy.deleted.message");
            }
            throw new ConflictException("project.deploy.conflict.message");
        }

        var query = DeploymentCriteriaQuery.builder()
                .repository(deploymentId.getRepository())
                .name(deploymentId.getProjectName())
                .build();

        var deployments = getDeployments(query);
        if (deployments.size() > 1) {
            throw new ProjectException(
                    String.format("Multiple deployments found for name '%s' in repository '%s'.", deploymentId.getProjectName(),
                            deploymentId.getRepository()));
        }

        if (deployments.isEmpty()) {
            if (!aclProjectsHelper.hasCreateDeploymentPermission(deploymentId.getRepository())) {
                throw new ForbiddenException("default.message");
            }
        }

        var deploymentRequest = DeploymentRequest.builder()
                .productionRepositoryId(deploymentId.getRepository())
                .name(deploymentId.getProjectName())
                .currentUser(getUserWorkspace().getUser())
                .comment(comment);

        var projectDescriptors = Stream.concat(Stream.of(project), projectDependencyResolver.getProjectDependencies(project).stream())
                .map(ProjectDescriptorImpl::from)
                .toList();
        deploymentRequest.projectDescriptors(projectDescriptors);
        var request = deploymentRequest.build();

        var validBranchName = deploymentManager.validateOnMainBranch(request);
        if (validBranchName != null) {
            throw new ConflictException("project.deploy.restricted.message", validBranchName);
        }
        if (!aclProjectsHelper.hasPermission(request, BasePermission.WRITE)) {
            throw new ForbiddenException("default.message");
        }
        deploymentManager.deploy(request);
    }

    private UserWorkspace getUserWorkspace() {
        return userWorkspaceProvider.getObject();
    }
}
