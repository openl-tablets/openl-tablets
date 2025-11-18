package org.openl.rules.webstudio.repositories.mcp.controller;

import java.io.IOException;
import java.util.List;

import org.springaicommunity.mcp.annotation.McpTool;

import org.openl.rules.rest.model.RepositoryFeatures;
import org.openl.rules.rest.model.RepositoryViewModel;
import org.openl.rules.webstudio.repositories.mcp.model.ListRepositoriesRequest;
import org.openl.rules.webstudio.repositories.mcp.model.ListRepositoryBranchesRequest;
import org.openl.rules.webstudio.repositories.mcp.model.ListRepositoryFeaturesRequest;
import org.openl.rules.webstudio.repositories.service.DesignTimeRepositoryService;
import org.openl.rules.webstudio.repositories.service.ProductionRepositoryService;
import org.openl.studio.mcp.McpController;

@McpController
public class RepositoriesMcpToolController {

    private final DesignTimeRepositoryService designTimeRepositoryService;
    private final ProductionRepositoryService productionRepositoryService;

    public RepositoriesMcpToolController(DesignTimeRepositoryService designTimeRepositoryService,
                                         ProductionRepositoryService productionRepositoryService) {
        this.designTimeRepositoryService = designTimeRepositoryService;
        this.productionRepositoryService = productionRepositoryService;
    }

    @McpTool(name = "repositories-list", description = "Returns all repositories by type in OpenL Tablets.\nReturns repository names, their types, and status information.\nUse this to discover all available repositories before accessing projects in the Design Repository or before deploying projects to the Production Repository.")
    public List<RepositoryViewModel> listRepositories(ListRepositoriesRequest request) {
        return switch (request.type()) {
            case DESIGN -> designTimeRepositoryService.getRepositoryList();
            case PROD -> productionRepositoryService.getRepositoryList();
        };
    }

    @McpTool(name = "repositories-get-branches", description = "Returns a list of branches for the specified repository. NOTE: Currently supported only for Design repositories.")
    public List<String> listBranches(ListRepositoryBranchesRequest request) throws IOException {
        return designTimeRepositoryService.getBranches(request.repoId());
    }

    @McpTool(name = "repositories-get-features", description = "Returns the features supported by the specified repository. NOTE: Currently supported only for Design repositories.")
    public RepositoryFeatures getRepositoryFeatures(ListRepositoryFeaturesRequest request) {
        return designTimeRepositoryService.getFeatures(request.repoId());
    }
}
