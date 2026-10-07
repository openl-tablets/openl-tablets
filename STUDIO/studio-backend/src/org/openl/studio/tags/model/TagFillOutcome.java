package org.openl.studio.tags.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What filling tags from the project name templates did to one project it was asked for.
 *
 * @author Yury Molchan
 */
@Schema(description = "What filling tags from the project name templates did to a project")
public enum TagFillOutcome {

    /**
     * The tags file of the project was written: committed for a closed project, put into the working copy of an
     * opened one.
     */
    @Schema(description = "The tags file of the project was written")
    @JsonProperty("updated")
    UPDATED,

    /**
     * The current user cannot change the project now, so it was left alone.
     */
    @Schema(description = "The project cannot be changed now, so it was left alone")
    @JsonProperty("notModifiable")
    NOT_MODIFIABLE,

    /**
     * No derived value the project lacks can be assigned, so it was left alone.
     */
    @Schema(description = "No derived value the project lacks can be assigned, so it was left alone")
    @JsonProperty("nothingToAssign")
    NOTHING_TO_ASSIGN,

    /**
     * The tags file could not be written; the log says why.
     */
    @Schema(description = "The tags file could not be written")
    @JsonProperty("failed")
    FAILED,
}
