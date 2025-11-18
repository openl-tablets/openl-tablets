package org.openl.rules.webstudio.projects.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

public record CreateProjectBranchRequest(
        @McpToolParam(description = "Project identifier")
        String projectId,
        @McpToolParam(description = "Branch name to create")
        String branch,
        @McpToolParam(description = "Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used.", required = false)
        String revision
) {
}
