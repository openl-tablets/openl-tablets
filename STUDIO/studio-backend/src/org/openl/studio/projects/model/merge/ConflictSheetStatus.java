package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How the merge takes a sheet of a conflicted workbook by itself.
 */
@Schema(description = "How the merge takes a sheet of a conflicted workbook by itself")
public enum ConflictSheetStatus {
    /**
     * Only your branch changed the sheet, so it is taken from your version.
     */
    @Schema(description = "Only your branch changed the sheet, so it is taken from your version")
    @JsonProperty("ours")
    OURS,

    /**
     * Only their branch changed the sheet, so it is taken from their version.
     */
    @Schema(description = "Only their branch changed the sheet, so it is taken from their version")
    @JsonProperty("theirs")
    THEIRS,

    /**
     * Both branches changed the sheet in ways the merge combines.
     */
    @Schema(description = "Both branches changed the sheet in ways the merge combines")
    @JsonProperty("merged")
    MERGED,

    /**
     * Both branches changed the sheet in ways the merge cannot combine.
     */
    @Schema(description = "Both branches changed the sheet in ways the merge cannot combine")
    @JsonProperty("conflict")
    CONFLICT
}
