package org.openl.studio.projects.model.tables;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateNewTableRequest(
        @Schema(description = "Name of the module where the table will be created")
        @NotBlank
        String moduleName,

        @Schema(description = "Name of the sheet where the table will be created")
        String sheetName,

        @NotNull
        @Valid
        EditableTableView table
) {
}
