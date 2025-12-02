package org.openl.studio.deployment.mcp.controller;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.studio.deployment.model.DeployProjectModel;
import org.openl.studio.deployment.model.DeploymentViewModel;
import org.openl.studio.deployment.model.RedeployProjectModel;
import org.openl.studio.deployment.service.DeploymentCriteriaQuery;
import org.openl.studio.deployment.service.DeploymentService;
import org.openl.studio.mcp.McpController;
import org.openl.studio.mcp.McpToolNameConstants;
import org.openl.studio.projects.converter.Base64ProjectConverter;
import org.openl.studio.projects.model.ProjectIdModel;

@McpController
public class DeploymentsMcpController {

    private final DeploymentService deploymentService;
    private final Base64ProjectConverter projectConverter;

    public DeploymentsMcpController(DeploymentService deploymentService, Base64ProjectConverter projectConverter) {
        this.deploymentService = deploymentService;
        this.projectConverter = projectConverter;
    }

    @Tool(name = McpToolNameConstants.LIST_TOOL_PREFIX + "_deployments", description = "Returns a list of deployments.")
    public List<DeploymentViewModel> getDeployments(@ToolParam(description = "Production repository id to filter deployments")
                                                    String repoId) {
        var query = DeploymentCriteriaQuery.builder()
                .repository(repoId)
                .build();
        return deploymentService.getDeployments(query).stream()
                .map(this::mapToViewModel)
                .toList();
    }

    @Tool(name = McpToolNameConstants.TOOL_PREFIX + "_deploy_project", description = "Deploys a project to the specified deployment target.")
    public void deploy(@ToolParam(description = "Deployment details") DeployProjectModel deployProject) throws ProjectException {
        var deploymentId = ProjectIdModel.builder()
                .repository(deployProject.productionRepositoryId)
                .projectName(deployProject.deploymentName)
                .build();
        var projectToDeploy = projectConverter.convert(deployProject.projectId.encode());
        deploymentService.deploy(deploymentId, projectToDeploy, deployProject.comment);
    }

    @Tool(name = McpToolNameConstants.TOOL_PREFIX + "_redeploy_project", description = "Redeploys a project to the specified deployment.")
    public void redeploy(@ToolParam(description = "Deployment identifier") String deploymentId,
                         @ToolParam(description = "Deployment details") RedeployProjectModel redeployProject) throws ProjectException {
        var id = ProjectIdModel.decode(deploymentId);
        var projectToDeploy = projectConverter.convert(redeployProject.projectId.encode());
        deploymentService.deploy(id, projectToDeploy, redeployProject.comment);
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
