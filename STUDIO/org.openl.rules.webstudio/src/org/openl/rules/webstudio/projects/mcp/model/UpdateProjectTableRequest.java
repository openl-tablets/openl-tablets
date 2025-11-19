package org.openl.rules.webstudio.projects.mcp.model;

import org.springframework.ai.tool.annotation.ToolParam;

import org.openl.rules.rest.model.tables.EditableTableView;

public record UpdateProjectTableRequest(
        @ToolParam(description = "Project identifier")
        String projectId,
        @ToolParam(description = "Table identifier")
        String tableId,
        @ToolParam(description = "Editable table data")
        EditableTableView editTable
) {
}
