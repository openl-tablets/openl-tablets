package org.openl.studio.projects.model.tables;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Request to copy an existing table into a module of the same project.
 * <p>
 * The table to copy is named by the id in the path, so the whole table content stays on the server and never crosses
 * the network. The request carries only the destination and the fields the author edits: the copy's name, its module
 * and sheet, an optional path for a new module, and the properties the copy should have.
 *
 * @param moduleName name of the module the copy is created in
 * @param sheetName  name of the sheet the copy is created in; defaults to the copy's name
 * @param modulePath project-relative path for a new module; when absent, the module must already exist
 * @param name       name of the copied table
 * @param properties the copy's properties; when omitted, the source table's properties are kept
 * @author Vladyslav Pikus
 */
public record CopyTableRequest(
        @Parameter(description = "Name of the module the copy is created in")
        @NotBlank
        @NonNull String moduleName,

        @Parameter(description = "Name of the sheet the copy is created in; defaults to the copy's name")
        @Nullable String sheetName,

        @Parameter(description = "Project-relative path for a new module. When absent, the module must already exist.")
        @Pattern(regexp = "(?i).+\\.xlsx$")
        @Nullable String modulePath,

        @Parameter(description = "Name of the copied table")
        @NotBlank
        @NonNull String name,

        @Parameter(description = "The copy's properties. When omitted, the source table's properties are kept.")
        @Valid
        @Nullable List<TableProperty> properties
) {
}
