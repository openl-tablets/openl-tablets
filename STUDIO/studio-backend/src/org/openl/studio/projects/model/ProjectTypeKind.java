package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a type of a project declares: fields, or the values it accepts.
 *
 * @author Yury Molchan
 */
@Schema(description = "Whether a type declares fields or values")
public enum ProjectTypeKind {

    @Schema(description = "A datatype, which declares fields.")
    @JsonProperty("datatype")
    DATATYPE,

    @Schema(description = "A vocabulary, which declares the values it accepts.")
    @JsonProperty("vocabulary")
    VOCABULARY
}
