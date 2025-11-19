package org.openl.rules.webstudio.repositories.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record ListRepositoryBranchesRequest(
        @ToolParam(description = "Repository identifier")
        String repoId) {
}
