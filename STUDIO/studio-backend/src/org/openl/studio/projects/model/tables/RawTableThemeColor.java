package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;

/**
 * A colour as one of the sixty colours the palette of Excel offers for the theme of a workbook: a theme colour, made
 * lighter or darker by a tint.
 *
 * <p>Every colour of the table theme is one, and so is a colour a cell is styled with from the palette. A screen draws
 * it in the colours of a theme of its own by the theme colour and the tint, as Excel draws it in the colours of the
 * theme of a workbook.
 *
 * @param name the theme colour
 * @param tint how much lighter the colour is, above 0, or darker, below 0, from -1 to 1; absent when the colour is the
 *             theme colour itself
 * @author Yury Molchan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Colour as a theme colour of Excel, made lighter or darker")
public record RawTableThemeColor(
        @Parameter(description = "Theme colour of Excel")
        @NotNull
        RawTableThemeColorName name,

        @Parameter(description = "How much lighter, above 0, or darker, below 0, from -1 to 1, as the palette of Excel "
                + "names it: 0.6 for Lighter 60%, -0.25 for Darker 25%; absent for the theme colour itself")
        @DecimalMin("-1")
        @DecimalMax("1")
        @Nullable Double tint
) {

    /** How many thousandths of a tint make it whole. */
    private static final double THOUSANDTHS = 1000.0;

    /**
     * A colour of the palette of Excel as the Tables API reports it.
     *
     * @param colour the colour, one of the ten theme colours made lighter or darker
     * @return the colour
     */
    public static RawTableThemeColor of(ThemedColor colour) {
        return new RawTableThemeColor(RawTableThemeColorName.values()[colour.index()],
                colour.tint() == 0 ? null : colour.tint() / THOUSANDTHS);
    }

    /**
     * The colour as a workbook writes it: the theme colour, made lighter or darker by the tint.
     *
     * @return the theme colour with its tint in thousandths
     */
    public ThemedColor themed() {
        return new ThemedColor(name.ordinal(), tint == null ? 0 : (int) Math.round(tint * THOUSANDTHS));
    }
}
