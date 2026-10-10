package org.openl.studio.projects.service.tables.read;

import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.rules.tableeditor.model.ui.BorderStyle;
import org.openl.studio.projects.model.tables.RawTableCellBorder;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableHorizontalAlign;
import org.openl.studio.projects.model.tables.RawTableStyleSource;
import org.openl.studio.projects.model.tables.RawTableThemeColor;
import org.openl.studio.projects.model.tables.RawTableVerticalAlign;
import org.openl.studio.projects.service.tables.theme.ThemeBorder;
import org.openl.studio.projects.service.tables.theme.ThemeBorderLine;
import org.openl.studio.projects.service.tables.theme.ThemeHorizontalAlign;
import org.openl.studio.projects.service.tables.theme.ThemeStyle;
import org.openl.studio.projects.service.tables.theme.ThemeVerticalAlign;

/**
 * Turns the look of the theme into the cell style the Tables API reports.
 *
 * <p>The style reported leaves out an attribute at its default, as the style read from the workbook does: a white
 * background, a black font, the left and bottom alignment, and a font that is not bold.
 *
 * <p>A colour the theme sets is reported as the colour of the palette of Excel it is, so a screen can draw it in the
 * colours of its own theme. A colour left out at its default is left out both ways.
 *
 * <p>The style is the look of the theme alone: nothing of the formatting of the workbook is reported with it, but the
 * indent, which sets out the structure of a table, such as the steps of a TBasic algorithm. Every style the theme
 * gives a cell, or a piece of its text, names the theme as its source ({@link RawTableStyleSource#THEME}).
 */
final class ThemeStyles {

    private ThemeStyles() {
    }

    /**
     * The style the theme gives a cell.
     *
     * @param theme  the look the theme gives the cell
     * @param indent the indent the cell has in the workbook, or {@code null} for none
     * @return the cell style of the look, with the indent of the cell, the theme named as its source
     */
    static RawTableCellStyle of(ThemeStyle theme, @Nullable Integer indent) {
        var builder = RawTableCellStyle.builder()
                .source(RawTableStyleSource.THEME)
                .indent(indent);
        if (theme.background() != null) {
            var background = colour(theme.background().rgb(), RawTableStyles.WHITE);
            builder.background(background)
                    .backgroundTheme(themed(background, theme.background().themed()));
        }
        Optional.ofNullable(theme.align()).ifPresent(align -> builder.align(horizontal(align)));
        Optional.ofNullable(theme.valign()).ifPresent(valign -> builder.valign(vertical(valign)));
        setFont(builder, theme);
        if (theme.border() != null) {
            var border = border(theme.border());
            builder.border(border.isEmpty() ? null : border);
        }
        return builder.build();
    }

    /**
     * The font the theme gives a piece of text.
     *
     * @param theme the look of the piece
     * @return the font of the piece, the theme named as its source
     */
    static RawTableCellStyle fontOf(ThemeStyle theme) {
        var builder = RawTableCellStyle.builder().source(RawTableStyleSource.THEME);
        setFont(builder, theme);
        return builder.build();
    }

    /** Sets the font attributes the theme names, its colour among them. */
    private static void setFont(RawTableCellStyle.RawTableCellStyleBuilder builder, ThemeStyle theme) {
        if (theme.color() != null) {
            var color = colour(theme.color().rgb(), RawTableStyles.BLACK);
            builder.color(color).colorTheme(themed(color, theme.color().themed()));
        }
        Optional.ofNullable(theme.bold()).ifPresent(on -> builder.bold(RawTableStyles.flag(on)));
        Optional.ofNullable(theme.italic()).ifPresent(on -> builder.italic(RawTableStyles.flag(on)));
        Optional.ofNullable(theme.underline()).ifPresent(on -> builder.underline(RawTableStyles.flag(on)));
        Optional.ofNullable(theme.strikeout()).ifPresent(on -> builder.strikeout(RawTableStyles.flag(on)));
        Optional.ofNullable(theme.fontFamily()).ifPresent(builder::fontFamily);
        Optional.ofNullable(theme.fontSize()).ifPresent(builder::fontSize);
    }

    /** A colour as the Tables API reports it, or {@code null} when it is the default. */
    private static @Nullable String colour(String hex, String defaultHex) {
        return RawTableStyles.hex(PoiExcelHelper.toRgb(hex), defaultHex);
    }

    /** The colour of the palette of Excel a colour is, or {@code null} when the colour is left out. */
    private static @Nullable RawTableThemeColor themed(@Nullable String hex, ThemedColor colour) {
        return hex == null ? null : RawTableThemeColor.of(colour);
    }

    private static @Nullable RawTableHorizontalAlign horizontal(ThemeHorizontalAlign align) {
        return switch (align) {
            case LEFT -> null;
            case CENTER -> RawTableHorizontalAlign.CENTER;
            case RIGHT -> RawTableHorizontalAlign.RIGHT;
            case JUSTIFY -> RawTableHorizontalAlign.JUSTIFY;
        };
    }

    private static @Nullable RawTableVerticalAlign vertical(ThemeVerticalAlign valign) {
        return switch (valign) {
            case TOP -> RawTableVerticalAlign.TOP;
            case CENTER -> RawTableVerticalAlign.CENTER;
            case BOTTOM -> null;
        };
    }

    /** The cell borders the theme names. */
    private static RawTableCellBorder border(ThemeBorder theme) {
        var builder = RawTableCellBorder.builder();
        side(theme.top(), builder::top);
        side(theme.right(), builder::right);
        side(theme.bottom(), builder::bottom);
        side(theme.left(), builder::left);
        return builder.build();
    }

    /** Sets one border side, drawn with the line the workbook draws it with once the theme is written. */
    private static void side(@Nullable ThemeBorderLine line,
                             Function<RawTableCellBorderSide, RawTableCellBorder.RawTableCellBorderBuilder> into) {
        Optional.ofNullable(line).ifPresent(drawn -> into.apply(reported(drawn)));
    }

    /**
     * The line a look draws over a cell, as a read reports it, or {@code null} when the look draws none there.
     *
     * @param theme the look of the cell
     * @return the line over the cell
     */
    static @Nullable RawTableCellBorderSide topLine(ThemeStyle theme) {
        return Optional.ofNullable(theme.border()).map(ThemeBorder::top).map(ThemeStyles::reported).orElse(null);
    }

    /** A line as a read reports it: the line the workbook draws once the theme is written, or none at all. */
    private static @Nullable RawTableCellBorderSide reported(ThemeBorderLine line) {
        var side = RawTableStyles.borderSide(BorderStyle.of(line.style().getExcel(), line.rgb()));
        var colour = line.color();
        return side == null || colour == null ? side
                : side.toBuilder().colorTheme(themed(side.color(), colour.themed())).build();
    }
}
