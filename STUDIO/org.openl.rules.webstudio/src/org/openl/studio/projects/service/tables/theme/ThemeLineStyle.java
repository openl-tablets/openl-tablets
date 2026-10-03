package org.openl.studio.projects.service.tables.theme;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;

/**
 * The line a theme draws a cell border with, named as Excel names it.
 */
@Getter
@RequiredArgsConstructor
public enum ThemeLineStyle {

    @JsonProperty("hair")
    HAIR(BorderStyle.HAIR),

    @JsonProperty("thin")
    THIN(BorderStyle.THIN),

    @JsonProperty("medium")
    MEDIUM(BorderStyle.MEDIUM),

    @JsonProperty("thick")
    THICK(BorderStyle.THICK),

    @JsonProperty("dashed")
    DASHED(BorderStyle.DASHED),

    @JsonProperty("dotted")
    DOTTED(BorderStyle.DOTTED),

    @JsonProperty("double")
    DOUBLE(BorderStyle.DOUBLE);

    /** The Excel border the line is written as. */
    private final BorderStyle excel;
}
