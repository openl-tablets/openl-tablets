package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The order the tables of a module are listed in.
 *
 * <p>A reader looking for one table by name reads a list sorted by name. A reader working through a module reads
 * it the way the module is written, which is what the Editor's tree draws.
 *
 * @author Vladyslav Pikus
 */
@Schema(description = "The order the tables of a module are listed in")
public enum TableSort {

    @Schema(description = "By name, ignoring case.")
    @JsonProperty("name")
    NAME,

    @Schema(description = "By where each table stands: workbook by workbook, sheet by sheet, and, within a sheet, "
            + "top to bottom and left to right. A table written across sheets stands after the sheets it is "
            + "gathered from.")
    @JsonProperty("position")
    POSITION
}
