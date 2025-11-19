package org.openl.rules.webstudio.repositories.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record GetProjectRevisionRequest(
        @ToolParam(description = "Repository identifier")
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
        int size
) {
}
