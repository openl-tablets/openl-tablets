package org.openl.studio.projects.model.tables;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;

/**
 * A colour of the palette of Excel, which is all a cell is coloured with: a theme colour of the workbook, made lighter
 * or darker, or one of the ten standard colours.
 *
 * <p>A theme colour is written as the theme colour itself, so the cell takes the colours of the theme of its
 * workbook. A workbook without a theme, such as an {@code .xls} one, takes the colour Office draws it in. A standard
 * colour is written as its RGB.
 *
 * <p>A colour is given one way only: a theme colour, with or without a tint, or an RGB, with no tint.
 *
 * @param theme the theme colour, or {@code null} for a standard colour
 * @param tint  how much lighter the theme colour is, above 0, or darker, below 0, from -1 to 1; absent for the theme
 *              colour itself
 * @param rgb   a standard colour as {@code #rrggbb}, or {@code null} for a theme colour
 * @author Yury Molchan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = """
        Colour of the palette of Excel: a theme colour of the workbook, made lighter or darker, or one of the ten \
        standard colours. Give either theme, with an optional tint, or rgb.""")
public record RawCellColor(
        @Parameter(description = "Theme colour of the workbook; not given together with rgb.")
        @Nullable RawTableThemeColorName theme,

        @Parameter(description = """
                How much lighter, above 0, or darker, below 0, the theme colour is, from -1 to 1, as the palette of \
                Excel names it: 0.6 for Lighter 60%, -0.25 for Darker 25%; absent for the theme colour itself.""")
        @DecimalMin("-1")
        @DecimalMax("1")
        @Nullable Double tint,

        @Parameter(description = """
                One of the ten standard colours of Excel as #rrggbb: #C00000, #FF0000, #FFC000, #FFFF00, #92D050, \
                #00B050, #00B0F0, #0070C0, #002060 or #7030A0; not given together with theme.""")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
        @Nullable String rgb
) {

    /** The ten standard colours of Excel, from Dark Red to Purple, as {@code #RRGGBB}. */
    public static final List<String> STANDARD = List.of("#C00000", "#FF0000", "#FFC000", "#FFFF00", "#92D050",
            "#00B050", "#00B0F0", "#0070C0", "#002060", "#7030A0");

    /** How many thousandths of a tint make it whole, as a workbook counts a tint. */
    private static final double THOUSANDTHS = 1000.0;

    /** Whether the colour is given one way: a theme colour, with or without its tint, or an RGB with no tint. */
    @JsonIgnore
    public boolean isOneColour() {
        return theme != null ? rgb == null : rgb != null && tint == null;
    }

    /** Whether the colour is a theme colour or one of the standard colours of Excel. */
    @JsonIgnore
    public boolean isInPalette() {
        return rgb == null || STANDARD.contains(rgb.toUpperCase(Locale.ROOT));
    }

    /**
     * The theme colour as a workbook writes it: the theme colour, made lighter or darker by the tint.
     *
     * @return the theme colour with its tint in thousandths
     * @throws NullPointerException for a standard colour, which has no theme colour
     */
    public ThemedColor themed() {
        return new ThemedColor(Objects.requireNonNull(theme, "theme").ordinal(),
                tint == null ? 0 : (int) Math.round(tint * THOUSANDTHS));
    }
}
