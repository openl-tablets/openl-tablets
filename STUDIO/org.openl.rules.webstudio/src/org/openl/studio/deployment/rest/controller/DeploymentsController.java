package org.openl.studio.deployment.rest.controller;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.deployment.model.DeployProjectModel;
import org.openl.studio.deployment.model.DeploymentViewModel;
import org.openl.studio.deployment.model.RedeployProjectModel;
import org.openl.studio.deployment.service.DeploymentCriteriaQuery;
import org.openl.studio.deployment.service.DeploymentService;
import org.openl.studio.projects.converter.Base64ProjectConverter;
import org.openl.studio.projects.model.ProjectIdModel;

@RestController
@RequestMapping(value = "/deployments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Deployments", description = "Deployment management APIs")
public class DeploymentsController {

    private final DeploymentService deploymentService;
    private final Base64ProjectConverter projectConverter;

    public DeploymentsController(DeploymentService deploymentService,
                                 Base64ProjectConverter projectConverter) {
        this.deploymentService = deploymentService;
        this.projectConverter = projectConverter;
    }

    @Operation(summary = "Get Deployments", description = "Returns a list of deployments. Optionally, filter by provided criterias.")
    @Parameters({
            @Parameter(name = "repository", description = "Production repository id to filter deployments", in = ParameterIn.QUERY)
    })
    @GetMapping
    @JsonView(GenericView.Short.class)
    public List<DeploymentViewModel> getDeployments(@RequestParam(value = "repository", required = false) String repository) {
        var query = DeploymentCriteriaQuery.builder()
                .repository(repository)
                .build();
        return deploymentService.getDeployments(query).stream()
                .map(this::mapToViewModel)
                .toList();
    }

    @Operation(summary = "Deploy Project", description = "Deploys a project to the specified deployment.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deploy(@RequestBody DeployProjectModel deployProject) throws ProjectException {
        var deploymentId = ProjectIdModel.builder()
                .repository(deployProject.productionRepositoryId)
                .projectName(deployProject.deploymentName)
                .build();
        var projectToDeploy = projectConverter.convert(deployProject.projectId.encode());
        deploymentService.deploy(deploymentId, projectToDeploy, deployProject.comment);
    }

    @Operation(summary = "Redeploy Project", description = "Redeploys a project to an existing deployment.")
    @PostMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redeploy(@PathVariable("id") String id, @RequestBody RedeployProjectModel redeployProject) throws ProjectException {
        var deploymentId = ProjectIdModel.decode(id);
        var projectToDeploy = projectConverter.convert(redeployProject.projectId.encode());
        deploymentService.deploy(deploymentId, projectToDeploy, redeployProject.comment);
    }

    private DeploymentViewModel mapToViewModel(Deployment deployment) {
        return DeploymentViewModel.builder()
                .id(ProjectIdModel.builder()
                        .repository(deployment.getRepository().getId())
                        .projectName(deployment.getDeploymentName())
                        .build())
                .name(deployment.getName())
                .repository(deployment.getRepository().getId())
                .build();
    }

}
