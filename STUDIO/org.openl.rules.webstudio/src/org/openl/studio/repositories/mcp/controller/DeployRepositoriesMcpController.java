package org.openl.studio.repositories.mcp.controller;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;

import org.openl.studio.mcp.McpController;
import org.openl.studio.repositories.model.RepositoryViewModel;
import org.openl.studio.repositories.service.DeployRepositoryService;

@McpController
public class DeployRepositoriesMcpController {

    private static final String TOOL_PREFIX = "deploy-repositories";

    private final DeployRepositoryService deploymentRepositoryService;

    public DeployRepositoriesMcpController(DeployRepositoryService deploymentRepositoryService) {
        this.deploymentRepositoryService = deploymentRepositoryService;
    }

    @Tool(name = TOOL_PREFIX + "-list", description = "Returns all Deployment Repositories in OpenL Tablets.\nReturns repository names, their types, and status information.\nUse this to discover all available Deployment repositories before accessing projects.")
    public List<RepositoryViewModel> listRepositories() {
        return deploymentRepositoryService.getRepositoryList();
    }
}
