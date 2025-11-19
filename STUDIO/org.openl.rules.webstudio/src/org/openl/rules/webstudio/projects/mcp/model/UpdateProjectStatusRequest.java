package org.openl.rules.webstudio.projects.mcp.model;

import java.util.Set;

import org.springframework.ai.tool.annotation.ToolParam;

public record UpdateProjectStatusRequest(
        @ToolParam(description = "Project identifier")
        String projectId,
        @ToolParam(description = "Project status to set")
        ProjectStatusToSet status,
        @ToolParam(description = "Branch name to switching to. Supported only if project repository supports branch feature.", required = false)
        String branch,
        @ToolParam(description = "Revision to switch to", required = false)
        String revision,
        @ToolParam(description = "Comment", required = false)
        String comment,
        @ToolParam(description = "The list of selected branches", required = false)
        Set<String> selectedBranches
) {
}
