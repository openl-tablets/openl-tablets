package org.openl.studio.repositories.mcp.controller;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;

import org.openl.studio.mcp.McpController;
import org.openl.studio.mcp.McpToolNameConstants;
import org.openl.studio.repositories.model.RepositoryViewModel;
import org.openl.studio.repositories.service.DeploymentRepositoryService;

@McpController
public class DeploymentRepositoriesMcpController {

    private final DeploymentRepositoryService deploymentRepositoryService;

    public DeploymentRepositoriesMcpController(DeploymentRepositoryService deploymentRepositoryService) {
        this.deploymentRepositoryService = deploymentRepositoryService;
    }

    @Tool(name = McpToolNameConstants.LIST_TOOL_PREFIX + "_deploy_repositories", description = "Returns all Deployment Repositories in OpenL Tablets.\nReturns repository names, their types, and status information.\nUse this to discover all available Deployment repositories before accessing projects.")
    public List<RepositoryViewModel> listRepositories() {
        return deploymentRepositoryService.getRepositoryList();
    }
}
