package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Horizontal alignment of a cell.
 *
 * <p>A cell read back carries none when it uses the default left alignment; a cell being written takes
 * {@code left} to put it back to that default.
 */
@Schema(description = "Horizontal alignment")
public enum RawTableHorizontalAlign {

    @JsonProperty("left")
    LEFT,

    @JsonProperty("right")
    RIGHT,

    @JsonProperty("center")
    CENTER,

    @JsonProperty("justify")
    JUSTIFY
}
