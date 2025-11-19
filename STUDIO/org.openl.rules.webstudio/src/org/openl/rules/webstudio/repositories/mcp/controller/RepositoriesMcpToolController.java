package org.openl.rules.webstudio.repositories.mcp.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Page;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.PageResponse;
import org.openl.rules.rest.model.ProjectRevision;
import org.openl.rules.rest.model.RepositoryFeatures;
import org.openl.rules.rest.model.RepositoryViewModel;
import org.openl.rules.webstudio.repositories.mcp.model.GetProjectRevisionRequest;
import org.openl.rules.webstudio.repositories.mcp.model.ListRepositoriesRequest;
import org.openl.rules.webstudio.repositories.mcp.model.ListRepositoryBranchesRequest;
import org.openl.rules.webstudio.repositories.mcp.model.ListRepositoryFeaturesRequest;
import org.openl.rules.webstudio.repositories.service.DeploymentRepositoryService;
import org.openl.rules.webstudio.repositories.service.DesignTimeRepositoryService;
import org.openl.rules.webstudio.repositories.service.ProjectRevisionService;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.mcp.McpController;

@McpController
public class RepositoriesMcpToolController {

    private final DesignTimeRepositoryService designTimeRepositoryService;
    private final DeploymentRepositoryService deploymentRepositoryService;
    private final ProjectRevisionService projectRevisionService;
    private final DesignTimeRepository designTimeRepository;

    public RepositoriesMcpToolController(DesignTimeRepositoryService designTimeRepositoryService,
                                         DeploymentRepositoryService deploymentRepositoryService,
                                         ProjectRevisionService projectRevisionService,
                                         DesignTimeRepository designTimeRepository) {
        this.designTimeRepositoryService = designTimeRepositoryService;
        this.deploymentRepositoryService = deploymentRepositoryService;
        this.projectRevisionService = projectRevisionService;
        this.designTimeRepository = designTimeRepository;
    }

    @Tool(name = "repositories-list", description = "Returns all repositories by type in OpenL Tablets.\nReturns repository names, their types, and status information.\nUse this to discover all available repositories before accessing projects in the Design Repository or before deploying projects to the Production Repository.")
    public List<RepositoryViewModel> listRepositories(ListRepositoriesRequest request) {
        return switch (request.type()) {
            case DESIGN -> designTimeRepositoryService.getRepositoryList();
            case PROD -> deploymentRepositoryService.getRepositoryList();
        };
    }

    @Tool(name = "repositories-get-branches", description = "Returns a list of branches for the specified repository. NOTE: Currently supported only for Design repositories.")
    public List<String> listBranches(ListRepositoryBranchesRequest request) throws IOException {
        return designTimeRepositoryService.getBranches(request.repoId());
    }

    @Tool(name = "repositories-get-features", description = "Returns the features supported by the specified repository. NOTE: Currently supported only for Design repositories.")
    public RepositoryFeatures getRepositoryFeatures(ListRepositoryFeaturesRequest request) {
        return designTimeRepositoryService.getFeatures(request.repoId());
    }

    @Tool(name = "repositories-get-project-revision", description = "Returns the revision history (commit history) of a project in the Design Repository.\nInclude pagination parameters to control which revisions are returned.\nOptionally filter by branch name and search term to find specific revisions.")
    public PageResponse<ProjectRevision> getProjectRevision(GetProjectRevisionRequest request) throws IOException, ProjectException {
        var repository = designTimeRepository.getRepository(request.repoId());
        if (repository == null) {
            throw new NotFoundException("design.repo.message", request.repoId());
        }

        return projectRevisionService.getProjectRevision(
                repository,
                request.projectName(),
                request.branch(),
                request.search(),
                request.techRevs(),
                Page.of(request.page(), request.size()));
    }
}
