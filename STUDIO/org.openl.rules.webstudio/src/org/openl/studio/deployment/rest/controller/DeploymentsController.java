package org.openl.studio.deployment.rest.controller;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.repository.api.FileData;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.common.utils.AuditFields;
import org.openl.studio.deployment.model.DeployProjectModel;
import org.openl.studio.deployment.model.DeploymentItemViewModel;
import org.openl.studio.deployment.model.DeploymentViewModel;
import org.openl.studio.deployment.model.RedeployProjectModel;
import org.openl.studio.deployment.service.DeploymentCriteriaQuery;
import org.openl.studio.deployment.service.DeploymentService;
import org.openl.studio.projects.converter.ProjectIdentityConverter;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.security.CommitInfoRequired;
import org.openl.util.StringUtils;

@RestController
@RequestMapping(value = "/deployments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Deployments", description = "Deployment management APIs")
@RequiredArgsConstructor
@Slf4j
public class DeploymentsController {

    private final DeploymentService deploymentService;
    private final ProjectIdentityConverter projectConverter;

    @Operation(
            summary = "deployments.get-list.summary",
            description = "deployments.get-list.desc"
    )
    @Parameters({
            @Parameter(
                    name = "repository",
                    description = "deployments.get-list.param.repository.desc",
                    in = ParameterIn.QUERY
            ),
            @Parameter(
                    name = "project",
                    description = "deployments.get-list.param.project.desc",
                    in = ParameterIn.QUERY
            )
    })
    @GetMapping
    @JsonView(GenericView.Short.class)
    public List<DeploymentViewModel> getDeployments(
            @RequestParam(value = "repository", required = false) String repository,
            @RequestParam(value = "project", required = false) String project) {
        var query = DeploymentCriteriaQuery.builder()
                .repository(repository)
                .build();
        var deployments = deploymentService.getDeployments(query);
        if (StringUtils.isBlank(project)) {
            return deployments.stream()
                    .map(this::mapToViewModel)
                    .toList();
        }
        return deployments.stream()
                .map(deployment -> mapToProjectDeployment(deployment, project))
                .flatMap(Optional::stream)
                .toList();
    }

    @Operation(summary = "deployments.get.summary", description = "deployments.get.desc")
    @GetMapping("/{id}")
    @JsonView(GenericView.Full.class)
    public DeploymentViewModel getDeployment(
            @Parameter(description = "deployments.get.param.id.desc") @PathVariable("id") String id) {
        var deploymentId = decodeDeploymentId(id);
        var query = DeploymentCriteriaQuery.builder()
                .repository(deploymentId.getRepository())
                .build();
        return deploymentService.getDeployments(query).stream()
                .filter(deployment -> deployment.getDeploymentName().equals(deploymentId.getProjectName()))
                .findFirst()
                .map(this::mapToFullViewModel)
                .orElseThrow(() -> new NotFoundException("deployment.not-found.message"));
    }

    @Operation(summary = "deployments.deploy.summary", description = "deployments.deploy.desc")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @CommitInfoRequired
    public void deploy(@Valid @RequestBody DeployProjectModel deployProject) throws ProjectException {
        var deploymentId = ProjectIdModel.builder()
                .repository(deployProject.productionRepositoryId)
                .projectName(deployProject.deploymentName)
                .build();
        var projectToDeploy = projectConverter.convert(deployProject.projectId.encode());
        deploymentService.deploy(deploymentId, projectToDeploy, deployProject.comment);
    }

    @Operation(summary = "deployments.redeploy.summary", description = "deployments.redeploy.desc")
    @PostMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @CommitInfoRequired
    public void redeploy(@Parameter(description = "deployments.redeploy.param.id.desc") @PathVariable("id") String id,
                         @Valid @RequestBody RedeployProjectModel redeployProject) throws ProjectException {
        var deploymentId = decodeDeploymentId(id);
        var projectToDeploy = projectConverter.convert(redeployProject.projectId.encode());
        deploymentService.deploy(deploymentId, projectToDeploy, redeployProject.comment);
    }

    /**
     * Reads a deployment id from a URL path segment.
     *
     * <p>A truncated or hand-edited id is a client error, so it answers "not found" instead of failing
     * the request as a server fault.
     */
    private static ProjectIdModel decodeDeploymentId(String id) {
        try {
            return ProjectIdModel.decode(id);
        } catch (IllegalArgumentException e) {
            log.debug("Malformed deployment id '{}'", id, e);
            throw new NotFoundException("deployment.not-found.message");
        }
    }

    private DeploymentViewModel mapToViewModel(Deployment deployment) {
        return builder(deployment)
                .build();
    }

    private Optional<DeploymentViewModel> mapToProjectDeployment(Deployment deployment, String projectName) {
        var items = deployment.getProjects().stream()
                .filter(project -> project.getName().equals(projectName))
                .map(DeploymentsController::mapToItem)
                .toList();
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(builder(deployment)
                .items(items)
                .build());
    }

    private DeploymentViewModel.DeploymentViewModelBuilder builder(Deployment deployment) {
        return DeploymentViewModel.builder()
                .id(ProjectIdModel.builder()
                        .repository(deployment.getRepository().getId())
                        .projectName(deployment.getDeploymentName())
                        .build())
                .name(deployment.getName())
                .repository(deployment.getRepository().getId());
    }

    private DeploymentViewModel mapToFullViewModel(Deployment deployment) {
        return builder(deployment)
                .items(deployment.getProjects().stream().map(DeploymentsController::mapToItem).toList())
                .build();
    }

    private static DeploymentItemViewModel mapToItem(IProject project) {
        var projectName = project.getName();
        var builder = DeploymentItemViewModel.builder().name(projectName);
        fileDataOf(project, projectName).ifPresent(fileData -> {
            AuditFields.apply(fileData, builder::modifiedBy, builder::modifiedAt, builder::revision);
        });
        return builder.build();
    }

    private static Optional<FileData> fileDataOf(IProject project, String projectName) {
        if (!(project instanceof AProject aProject)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(aProject.getFileData());
        } catch (IllegalStateException e) {
            log.warn("Failed to read deployed project metadata for '{}'. Omitting audit fields.",
                    projectName, e);
            return Optional.empty();
        }
    }

}
