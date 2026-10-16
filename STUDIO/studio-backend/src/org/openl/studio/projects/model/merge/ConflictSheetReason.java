package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Why a sheet of a conflicted workbook is resolved only as a whole.
 */
@Schema(description = "Why a sheet of a conflicted workbook is resolved only as a whole")
public enum ConflictSheetReason {
    /**
     * One branch removed the sheet, and the other one changed it.
     */
    @Schema(description = "One branch removed the sheet, and the other one changed it")
    @JsonProperty("removedAndChanged")
    REMOVED_AND_CHANGED,

    /**
     * Both branches added a sheet of the same name with different content.
     */
    @Schema(description = "Both branches added a sheet of the same name with different content")
    @JsonProperty("addedInBoth")
    ADDED_IN_BOTH,

    /**
     * Both branches hid or showed the sheet differently.
     */
    @Schema(description = "Both branches hid or showed the sheet differently")
    @JsonProperty("visibility")
    VISIBILITY,

    /**
     * Both branches changed the pictures of the sheet differently.
     */
    @Schema(description = "Both branches changed the pictures of the sheet differently")
    @JsonProperty("pictures")
    PICTURES,

    /**
     * Their branch changed the data validation, the conditional formatting, the hyperlinks or the frozen panes.
     */
    @Schema(description = "Their branch changed the data validation, the conditional formatting, the hyperlinks or "
            + "the frozen panes, which the merge does not take")
    @JsonProperty("settings")
    SETTINGS,

    /**
     * Tables of the two branches would touch each other and be read as one table.
     */
    @Schema(description = "Tables of the two branches would touch each other and be read as one table")
    @JsonProperty("touchingTables")
    TOUCHING_TABLES,

    /**
     * Content that moves is referred to by a formula, a name, a data validation, a conditional formatting or a
     * hyperlink, or lies under a picture or another object.
     */
    @Schema(description = "Content that moves is referred to by a formula, a defined name, a data validation, a "
            + "conditional formatting or a hyperlink, or lies under a picture or another object")
    @JsonProperty("movedContent")
    MOVED_CONTENT
}
