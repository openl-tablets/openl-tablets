package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One of the ten theme colours of Excel: the first and the second background and text, and the six accents.
 *
 * <p>The constants stand in the order a workbook numbers the theme colours, so the number a workbook writes is the
 * ordinal of the constant.
 *
 * @author Yury Molchan
 */
@Schema(description = "Theme colour of Excel: lt1 and dk1 are the first background and text, lt2 and dk2 the second "
        + "ones, accent1 to accent6 the accents")
public enum RawTableThemeColorName {

    @JsonProperty("lt1")
    LT1,

    @JsonProperty("dk1")
    DK1,

    @JsonProperty("lt2")
    LT2,

    @JsonProperty("dk2")
    DK2,

    @JsonProperty("accent1")
    ACCENT1,

    @JsonProperty("accent2")
    ACCENT2,

    @JsonProperty("accent3")
    ACCENT3,

    @JsonProperty("accent4")
    ACCENT4,

    @JsonProperty("accent5")
    ACCENT5,

    @JsonProperty("accent6")
    ACCENT6
}
