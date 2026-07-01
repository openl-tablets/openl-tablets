package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Horizontal alignment of a cell; absent when the cell uses the default left alignment. */
@Schema(description = "Horizontal alignment")
public enum RawTableHorizontalAlign {

    @JsonProperty("right")
    RIGHT,

    @JsonProperty("center")
    CENTER,

    @JsonProperty("justify")
    JUSTIFY
}
