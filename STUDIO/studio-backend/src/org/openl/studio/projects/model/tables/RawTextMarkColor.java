package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The colour of a mark on a piece of a cell text.
 *
 * <p>Each colour is a functional colour of OpenL Studio. On the screen it takes the colour of the Studio theme, light
 * or dark. In the Excel file it is one of the standard colours of Excel, so the mark looks the same in Excel.
 *
 * @author Yury Molchan
 */
@Schema(description = "Colour of a mark on a piece of a cell text")
@RequiredArgsConstructor
public enum RawTextMarkColor {

    /** What is wrong or must not be missed: Red in Excel. */
    @JsonProperty("error")
    ERROR(0xFF0000),

    /** What needs care: Orange in Excel. */
    @JsonProperty("warning")
    WARNING(0xFFC000),

    /** What is right or confirmed: Green in Excel. */
    @JsonProperty("success")
    SUCCESS(0x00B050),

    /** What is worth a note: Light Blue in Excel. */
    @JsonProperty("info")
    INFO(0x00B0F0);

    /** The standard colour of Excel the mark is written in, as {@code 0xRRGGBB}. */
    @Getter
    private final int rgb;
}
