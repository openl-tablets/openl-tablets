package org.openl.studio.deployment.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentRequest;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.repository.cache.CachedProjectVersion;
import org.openl.rules.webstudio.web.repository.cache.ProjectVersionCacheManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ProjectDependencyResolver;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.util.StringUtils;

@RequiredArgsConstructor
@Slf4j
public class DeploymentServiceImpl implements DeploymentService {

    private static final String SEPARATOR = "#";

    private final ProjectDependencyResolver projectDependencyResolver;
    private final SecureDeploymentRepositoryService deploymentRepositoryService;
    private final DeploymentManager deploymentManager;
    private final ObjectProvider<UserWorkspace> userWorkspaceProvider;
    private final ProjectStateValidator projectStateValidator;
    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectVersionCacheManager projectVersionCacheManager;

    @Override
    public List<Deployment> getDeployments(DeploymentCriteriaQuery query) {
        Stream<RepositoryConfiguration> repoConfigsStream;
        if (StringUtils.isNotBlank(query.repository())) {
            repoConfigsStream = deploymentRepositoryService.getRepository(query.repository()).stream();
        } else {
            repoConfigsStream = deploymentRepositoryService.getRepositories().stream();
        }
        return repoConfigsStream.flatMap(this::deploymentsOf)
                .filter(query.getFilter())
                .sorted(RepositoryUtils.ARTEFACT_COMPARATOR)
                .toList();
    }

    /**
     * The deployments of one repository. A repository that cannot be instantiated or read — a
     * misconfigured connection, an unreachable server — yields none: the error is logged and the
     * listing goes on with the remaining repositories instead of failing as a whole.
     */
    private Stream<Deployment> deploymentsOf(RepositoryConfiguration config) {
        try {
            return listDeployments(config);
        } catch (Exception e) {
            log.error("Failed to read deployments from repository '{}'. The repository is skipped.",
                    config.getId(),
                    e);
            return Stream.empty();
        }
    }

    private Stream<Deployment> listDeployments(RepositoryConfiguration config) throws IOException {
        var repository = deploymentManager.getDeployRepository(config.getId());
        var basePath = deploymentManager.repositoryFactoryProxy.getBasePath(config.getId());

        var latestDeployments = new HashMap<String, Deployment>();
        var versionsList = new HashMap<String, Integer>();

        Collection<FileData> fileDatas;
        if (repository.supports().folders()) {
            // All deployments
            fileDatas = repository.listFolders(basePath);
        } else {
            // Projects inside all deployments
            fileDatas = repository.list(basePath);
        }
        for (FileData fileData : fileDatas) {
            var deploymentFolderName = fileData.getName().substring(basePath.length()).split("/")[0];
            var separatorPosition = deploymentFolderName.lastIndexOf(SEPARATOR);

            var deploymentName = deploymentFolderName;
            var version = 0;
            CommonVersionImpl commonVersion;
            if (separatorPosition >= 0) {
                deploymentName = deploymentFolderName.substring(0, separatorPosition);
                version = Integer.parseInt(deploymentFolderName.substring(separatorPosition + 1));
                commonVersion = new CommonVersionImpl(version);
            } else {
                commonVersion = new CommonVersionImpl(fileData.getVersion());
            }
            var previous = versionsList.put(deploymentName, version);
            if (previous != null && previous > version) {
                // rollback
                versionsList.put(deploymentName, previous);
            } else {
                // put the latest deployment
                var folderPath = basePath + deploymentFolderName;
                boolean folderStructure;
                if (repository.supports().folders()) {
                    folderStructure = !repository.listFolders(folderPath + "/").isEmpty();
                } else {
                    folderStructure = false;
                }
                var deployment = new Deployment(repository,
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
                    "Multiple deployments found for name '%s' in repository '%s'.".formatted(deploymentId.getProjectName(),
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

    /**
     * A project whose revision cannot be worked out — unreadable metadata, an unreachable version cache — costs
     * its own revision only: the error is logged and the rest of the deployment is still listed.
     */
    @Override
    public Optional<CachedProjectVersion> findDesignRevision(AProject deployedProject) {
        try {
            return Optional.ofNullable(projectVersionCacheManager.getDesignVersionOfDeployedProject(deployedProject));
        } catch (Exception e) {
            // A deploy repository that keeps no version info fails this way for every project, every listing,
            // so it is a property of the repository to note rather than an incident to raise.
            log.warn("Failed to find the design revision of deployed project '{}'.", deployedProject.getName(), e);
            return Optional.empty();
        }
    }

    private UserWorkspace getUserWorkspace() {
        return userWorkspaceProvider.getObject();
    }
}
