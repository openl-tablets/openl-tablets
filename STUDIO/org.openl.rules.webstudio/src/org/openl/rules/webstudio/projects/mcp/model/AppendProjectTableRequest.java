package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

import org.openl.rules.rest.model.tables.AppendTableView;

public record AppendProjectTableRequest(
        @ToolParam(description = "Project identifier")
        String projectId,
        @ToolParam(description = "Table identifier")
        String tableId,
        @ToolParam(description = "Appendable table data")
        AppendTableView editTable
) {
}
