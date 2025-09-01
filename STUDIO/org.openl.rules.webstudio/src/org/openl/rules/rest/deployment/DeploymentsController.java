package org.openl.rules.rest.deployment;

import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.rest.deployment.model.DeployProjectModel;
import org.openl.rules.rest.deployment.model.DeploymentViewModel;
import org.openl.rules.rest.deployment.model.RedeployProjectModel;
import org.openl.rules.rest.deployment.service.DeploymentCriteriaQuery;
import org.openl.rules.rest.deployment.service.DeploymentService;
import org.openl.rules.rest.model.ProjectIdModel;
import org.openl.rules.rest.resolver.Base64ProjectConverter;

@RestController
@RequestMapping(value = "/deployments", produces = MediaType.APPLICATION_JSON_VALUE)
@Hidden
public class DeploymentsController {

    private final DeploymentService deploymentService;
    private final Base64ProjectConverter projectConverter;

    public DeploymentsController(DeploymentService deploymentService,
                                 Base64ProjectConverter projectConverter) {
        this.deploymentService = deploymentService;
        this.projectConverter = projectConverter;
    }

    @GetMapping
    public List<DeploymentViewModel> getDeployments(@RequestParam(value = "repository", required = false) String repository) {
        var query = DeploymentCriteriaQuery.builder()
                .repository(repository)
                .build();
        return deploymentService.getDeployments(query).stream()
                .map(deployment -> mapToViewModel(deployment).build())
                .toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void redeploy(@RequestBody DeployProjectModel deployProject) throws ProjectException {
        var deploymentId = ProjectIdModel.builder()
                .repository(deployProject.productionRepositoryId)
                .projectName(deployProject.deploymentName)
                .build();
        var projectToDeploy = projectConverter.convert(deployProject.projectId.encode());
        deploymentService.deploy(deploymentId, projectToDeploy, deployProject.comment);
    }

    @PostMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void redeploy(@PathVariable("id") String id, @RequestBody RedeployProjectModel redeployProject) throws ProjectException {
        var deploymentId = ProjectIdModel.decode(id);
        var projectToDeploy = projectConverter.convert(redeployProject.projectId.encode());
        deploymentService.deploy(deploymentId, projectToDeploy, redeployProject.comment);
    }

    private DeploymentViewModel.Builder mapToViewModel(Deployment deployment) {
        return DeploymentViewModel.builder()
                .id(ProjectIdModel.builder()
                        .repository(deployment.getRepository().getId())
                        .projectName(deployment.getDeploymentName())
                        .build())
                .name(deployment.getName())
                .repository(deployment.getRepository().getId());
    }

}
