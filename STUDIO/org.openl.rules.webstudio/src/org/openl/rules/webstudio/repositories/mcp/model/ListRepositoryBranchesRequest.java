package org.openl.rules.webstudio.repositories.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

public record ListRepositoryBranchesRequest(
        @McpToolParam(description = "Repository identifier")
        String repoId) {
}
