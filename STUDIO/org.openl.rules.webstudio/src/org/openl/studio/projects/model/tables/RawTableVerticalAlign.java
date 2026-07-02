package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Vertical alignment of a cell; absent when the cell uses the default bottom alignment. */
@Schema(description = "Vertical alignment")
public enum RawTableVerticalAlign {

    @JsonProperty("center")
    CENTER,

    @JsonProperty("top")
    TOP
}
