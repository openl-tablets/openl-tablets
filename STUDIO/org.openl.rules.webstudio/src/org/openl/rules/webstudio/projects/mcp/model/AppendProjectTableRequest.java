package org.openl.rules.webstudio.projects.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

import org.openl.rules.rest.model.tables.AppendTableView;

public record AppendProjectTableRequest(
        @McpToolParam(description = "Project identifier")
        String projectId,
        @McpToolParam(description = "Table identifier")
        String tableId,
        @McpToolParam(description = "Appendable table data")
        AppendTableView editTable
) {
}
