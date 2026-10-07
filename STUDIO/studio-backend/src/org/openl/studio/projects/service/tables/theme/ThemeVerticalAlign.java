package org.openl.studio.projects.service.tables.theme;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * How a theme lines the text of a cell up from top to bottom.
 */
@Getter
@RequiredArgsConstructor
public enum ThemeVerticalAlign {

    @JsonProperty("top")
    TOP(VerticalAlignment.TOP),

    @JsonProperty("center")
    CENTER(VerticalAlignment.CENTER),

    @JsonProperty("bottom")
    BOTTOM(VerticalAlignment.BOTTOM);

    /** The Excel alignment the theme writes. */
    private final VerticalAlignment excel;
}
