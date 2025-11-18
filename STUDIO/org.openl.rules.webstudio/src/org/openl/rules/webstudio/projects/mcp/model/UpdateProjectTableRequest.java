package org.openl.rules.webstudio.projects.mcp.model;

import org.springaicommunity.mcp.annotation.McpToolParam;

import org.openl.rules.rest.model.tables.EditableTableView;

public record UpdateProjectTableRequest(
        @McpToolParam(description = "Project identifier")
        String projectId,
        @McpToolParam(description = "Table identifier")
        String tableId,
        @McpToolParam(description = "Editable table data")
        EditableTableView editTable
) {
}
