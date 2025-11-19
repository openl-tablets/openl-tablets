package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

public record CreateProjectBranchRequest(
        @ToolParam(description = "Project identifier")
        String projectId,
        @ToolParam(description = "Branch name to create")
        String branch,
        @ToolParam(description = "Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used.", required = false)
        String revision
) {
}
