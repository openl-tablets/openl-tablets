package org.openl.studio.projects.model.tables;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNewTableRequest(
        @NotBlank
        String moduleName,

        String sheetName,

        @NotNull
        @Valid
        EditableTableView table
) {
}
