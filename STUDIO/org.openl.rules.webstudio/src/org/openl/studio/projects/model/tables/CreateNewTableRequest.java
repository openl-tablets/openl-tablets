package org.openl.studio.projects.model.tables;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record CreateNewTableRequest(
        @Parameter(description = "Name of the module where the table will be created")
        @NotBlank
        @NonNull String moduleName,

        @Parameter(description = "Name of the sheet where the table will be created")
        @Nullable String sheetName,

        @Parameter(description = "Project-relative path for a new module. When absent, the module must already exist.")
        @Pattern(regexp = "(?i).+\\.xlsx$")
        @Nullable String modulePath,

        @Parameter(description = "Editable table content")
        @NotNull
        @Valid
        @NonNull EditableTableView table
) {
}
