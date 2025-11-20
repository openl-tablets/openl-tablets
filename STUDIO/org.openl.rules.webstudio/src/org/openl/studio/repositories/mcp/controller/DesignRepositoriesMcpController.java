package org.openl.studio.repositories.mcp.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Page;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.mcp.McpController;
import org.openl.studio.repositories.model.ProjectRevision;
import org.openl.studio.repositories.model.RepositoryFeatures;
import org.openl.studio.repositories.model.RepositoryViewModel;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;
import org.openl.studio.repositories.service.ProjectRevisionService;

@McpController
public class DesignRepositoriesMcpController {

    private static final String TOOL_PREFIX = "design-repositories";

    private final DesignTimeRepositoryService designTimeRepositoryService;
    private final ProjectRevisionService projectRevisionService;
    private final DesignTimeRepository designTimeRepository;

    public DesignRepositoriesMcpController(DesignTimeRepositoryService designTimeRepositoryService,
                                           ProjectRevisionService projectRevisionService,
                                           DesignTimeRepository designTimeRepository) {
        this.designTimeRepositoryService = designTimeRepositoryService;
        this.projectRevisionService = projectRevisionService;
        this.designTimeRepository = designTimeRepository;
    }

    @Tool(name = TOOL_PREFIX + "-list", description = "Returns all Design Repositories in OpenL Tablets.\nReturns repository names, their types, and status information.\nUse this to discover all available Design repositories before accessing projects.")
    public List<RepositoryViewModel> listRepositories() {
        return designTimeRepositoryService.getRepositoryList();
    }

    @Tool(name = TOOL_PREFIX + "-list-branches", description = "Returns a list of branches for the specified Design repository. It is applicable only for repositories that support branching. Use feature check to verify if the repository supports branches.")
    public List<String> listBranches(@ToolParam(description = "Design repository identifier") String repoId) throws IOException {
        return designTimeRepositoryService.getBranches(repoId);
    }

    @Tool(name = TOOL_PREFIX + "-list-features", description = "Returns the features supported by the specified Design repository.\nUse this to check if a repository supports specific features like branching before performing operations that depend on those features.")
    public RepositoryFeatures getRepositoryFeatures(@ToolParam(description = "Design repository identifier") String repoId) {
        return designTimeRepositoryService.getFeatures(repoId);
    }

    @Tool(name = TOOL_PREFIX + "-list-project-revisions", description = "Returns the revision history (commit history) of a project in the Design Repository.\nInclude pagination parameters to control which revisions are returned.\nOptionally filter by branch name and search term to find specific revisions.")
    public PageResponse<ProjectRevision> getProjectRevision(@ToolParam(description = "Design repository identifier")
                                                            String repoId,
                                                            @ToolParam(description = "Project name")
                                                            String projectName,
                                                            @ToolParam(description = "Branch name (optional, only if repository supports branches)", required = false)
                                                            String branch,
                                                            @ToolParam(description = "Search term to filter revisions by commit message or author", required = false)
                                                            String search,
                                                            @ToolParam(description = "Include technical revisions")
                                                            boolean techRevs,
                                                            @ToolParam(description = "Page number (0-based)")
                                                            int page,
                                                            @ToolParam(description = "Page size (number of results per page)")
                                                            int size) throws IOException, ProjectException {
        var repository = designTimeRepository.getRepository(repoId);
        if (repository == null) {
            throw new NotFoundException("design.repo.message", repoId);
        }

        return projectRevisionService.getProjectRevision(
                repository,
                projectName,
                branch,
                search,
                techRevs,
                Page.of(page, size));
    }

}
