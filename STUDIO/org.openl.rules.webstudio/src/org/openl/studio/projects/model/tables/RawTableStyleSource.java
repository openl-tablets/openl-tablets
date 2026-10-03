package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Where a style a raw read reports comes from: the workbook, or a table theme.
 *
 * <p>A read naming a table theme reports the look the theme gives a cell in place of the style of the workbook. That
 * look is a view only, which no edit writes. The workbook is the default, so a read leaves it out of a style, as it
 * leaves out every attribute at its default.
 */
@Schema(description = "Where a style comes from: the workbook, the default, or a table theme")
public enum RawTableStyleSource {

    @Schema(description = "The style the workbook holds; the default, left out of a read")
    @JsonProperty("workbook")
    WORKBOOK,

    @Schema(description = "The look the table theme the read named gives the cell, a view only")
    @JsonProperty("theme")
    THEME
}
