package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Border line style of a cell side. */
@Schema(description = "Border line style")
public enum RawTableBorderLineStyle {

    @JsonProperty("solid")
    SOLID,

    @JsonProperty("dashed")
    DASHED,

    @JsonProperty("dotted")
    DOTTED,

    @JsonProperty("double")
    DOUBLE
}
