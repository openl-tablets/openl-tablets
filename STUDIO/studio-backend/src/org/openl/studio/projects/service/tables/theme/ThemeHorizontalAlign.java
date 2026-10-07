package org.openl.studio.projects.service.tables.theme;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

/**
 * How a theme lines the text of a cell up across it.
 */
@Getter
@RequiredArgsConstructor
public enum ThemeHorizontalAlign {

    @JsonProperty("left")
    LEFT(HorizontalAlignment.LEFT),

    @JsonProperty("center")
    CENTER(HorizontalAlignment.CENTER),

    @JsonProperty("right")
    RIGHT(HorizontalAlignment.RIGHT),

    @JsonProperty("justify")
    JUSTIFY(HorizontalAlignment.JUSTIFY);

    /** The Excel alignment the theme writes. */
    private final HorizontalAlignment excel;
}
