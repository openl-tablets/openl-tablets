package org.openl.studio.tags.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What filling tags from a project name template does to one tag of one project.
 */
@Schema(description = "What filling tags from a project name template does to one tag of a project")
public enum TagFillState {

    /**
     * The derived value is configured for its tag type and is assigned to the project.
     */
    @Schema(description = "The derived value is configured and is assigned to the project")
    @JsonProperty("assign")
    ASSIGN,

    /**
     * The derived value is not configured yet, and the tag type is extensible: the value is created and
     * assigned to the project.
     */
    @Schema(description = "The derived value is created for its extensible tag type and assigned")
    @JsonProperty("create")
    CREATE,

    /**
     * The derived value is not configured and the tag type is not extensible: nothing is assigned, and the
     * project keeps the tag it has.
     */
    @Schema(description = "The derived value is not configured and its tag type does not take new values")
    @JsonProperty("rejected")
    REJECTED,

    /**
     * The project already carries the derived value: nothing changes.
     */
    @Schema(description = "The project already carries this value")
    @JsonProperty("keep")
    KEEP,
}
