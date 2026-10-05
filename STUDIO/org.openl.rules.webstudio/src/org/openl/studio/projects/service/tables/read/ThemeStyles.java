package org.openl.studio.projects.service.tables.read;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.tableeditor.model.ui.BorderStyle;
import org.openl.studio.projects.model.tables.RawTableCellBorder;
import org.openl.studio.projects.model.tables.RawTableCellBorderSide;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableHorizontalAlign;
import org.openl.studio.projects.model.tables.RawTableStyleSource;
import org.openl.studio.projects.model.tables.RawTableVerticalAlign;
import org.openl.studio.projects.service.tables.theme.ThemeBorder;
import org.openl.studio.projects.service.tables.theme.ThemeBorderLine;
import org.openl.studio.projects.service.tables.theme.ThemeColour;
import org.openl.studio.projects.service.tables.theme.ThemeHorizontalAlign;
import org.openl.studio.projects.service.tables.theme.ThemeStyle;
import org.openl.studio.projects.service.tables.theme.ThemeVerticalAlign;

/**
 * Turns the look of the theme into the cell style the Tables API reports.
 *
 * <p>The style reported leaves out an attribute at its default, as the style read from the workbook does: a white
 * background, a black font, the left and bottom alignment, and a font that is not bold.
 *
 * <p>A colour the theme sets is reported with the key of the theme file it is set at, such as
 * {@code spreadsheet.values.background}, so a screen can draw the colour of the key its own way, such as in the
 * colours of its own theme. A colour left out at its default is left out with its key.
 *
 * <p>Every style the theme gives a cell, or a piece of its text, names the theme as its source
 * ({@link RawTableStyleSource#THEME}): a read naming the theme reports it in place of the style of the workbook.
 */
final class ThemeStyles {

    private ThemeStyles() {
    }

    /**
     * Lays the look of the theme over a cell style.
     *
     * @param style the style the cell has in the workbook, or {@code null} when it has none
     * @param theme the look the theme gives the cell
     * @return the cell style with every attribute the theme sets replaced, the theme named as its source
     */
    static RawTableCellStyle over(@Nullable RawTableCellStyle style, ThemeStyle theme) {
        var builder = (style == null ? RawTableCellStyle.builder() : style.toBuilder())
                .source(RawTableStyleSource.THEME);
        if (theme.background() != null) {
            var background = colour(theme.background().rgb(), RawTableStyles.WHITE);
            builder.background(background).backgroundKey(keyOf(background, theme.background()));
        }
        if (theme.align() != null) {
            builder.align(horizontal(theme.align()));
        }
        if (theme.valign() != null) {
            builder.valign(vertical(theme.valign()));
        }
        setFont(builder, theme);
        if (theme.border() != null) {
            var border = border(style == null ? null : style.border(), theme.border());
            builder.border(border.isEmpty() ? null : border);
        }
        return builder.build();
    }

    /**
     * The font the theme gives a piece of text.
     *
     * <p>The piece starts from the font the cell has in the workbook, as writing the theme does, and takes every font
     * attribute the theme names. A run style names every attribute it sets, so the attributes of the cell font are
     * named on it as well.
     *
     * @param cell  the style the cell has in the workbook, or {@code null} when it has none
     * @param theme the look of the piece
     * @return the font of the piece, the theme named as its source
     */
    static RawTableCellStyle fontOf(@Nullable RawTableCellStyle cell, ThemeStyle theme) {
        var builder = RawTableCellStyle.builder().source(RawTableStyleSource.THEME);
        if (cell != null) {
            builder.color(cell.color())
                    .bold(cell.bold())
                    .italic(cell.italic())
                    .underline(cell.underline())
                    .strikeout(cell.strikeout());
        }
        setFont(builder, theme);
        return builder.build();
    }

    /** Sets the font attributes the theme names, its colour among them. */
    private static void setFont(RawTableCellStyle.RawTableCellStyleBuilder builder, ThemeStyle theme) {
        if (theme.color() != null) {
            var color = colour(theme.color().rgb(), RawTableStyles.BLACK);
            builder.color(color).colorKey(keyOf(color, theme.color()));
        }
        if (theme.bold() != null) {
            builder.bold(RawTableStyles.flag(theme.bold()));
        }
        if (theme.italic() != null) {
            builder.italic(RawTableStyles.flag(theme.italic()));
        }
        if (theme.underline() != null) {
            builder.underline(RawTableStyles.flag(theme.underline()));
        }
        if (theme.strikeout() != null) {
            builder.strikeout(RawTableStyles.flag(theme.strikeout()));
        }
        if (theme.fontFamily() != null) {
            builder.fontFamily(theme.fontFamily());
        }
        if (theme.fontSize() != null) {
            builder.fontSize(theme.fontSize());
        }
    }

    /** A colour as the Tables API reports it, or {@code null} when it is the default. */
    private static @Nullable String colour(String hex, String defaultHex) {
        return RawTableStyles.hex(PoiExcelHelper.toRgb(hex), defaultHex);
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

    /** The cell borders with every side the theme names replaced. */
    private static RawTableCellBorder border(@Nullable RawTableCellBorder cell, ThemeBorder theme) {
        var builder = cell == null ? RawTableCellBorder.builder() : cell.toBuilder();
        side(theme.top(), builder::top);
        side(theme.right(), builder::right);
        side(theme.bottom(), builder::bottom);
        side(theme.left(), builder::left);
        return builder.build();
    }

    /**
     * Sets one border side, drawn with the line the workbook draws it with once the theme is written. A side without
     * a line takes the border of the cell away.
     */
    private static void side(@Nullable ThemeBorderLine line,
                             Function<RawTableCellBorderSide, RawTableCellBorder.RawTableCellBorderBuilder> into) {
        if (line != null) {
            into.apply(reported(line));
        }
    }

    /**
     * The line a look draws over a cell, as a read reports it, or {@code null} when the look draws none there.
     *
     * @param theme the look of the cell
     * @return the line over the cell
     */
    static @Nullable RawTableCellBorderSide topLine(ThemeStyle theme) {
        var line = theme.border() == null ? null : theme.border().top();
        return line == null ? null : reported(line);
    }

    /**
     * A line as a read reports it: the line the workbook draws once the theme is written, or none at all. A line of a
     * colour the theme file sets keeps the key it is set at.
     */
    private static @Nullable RawTableCellBorderSide reported(ThemeBorderLine line) {
        var side = RawTableStyles.borderSide(BorderStyle.of(line.style().getExcel(), line.rgb()));
        if (side == null) {
            return null;
        }
        var key = keyOf(side.color(), line.color());
        return key == null ? side : side.toBuilder().colorKey(key).build();
    }

    /**
     * The key a colour is reported with: the key of the theme file the colour is set at, or none for a colour left
     * out at its default.
     *
     * @param reported the colour as the Tables API reports it, or {@code null} when it is left out
     * @param colour   the colour the theme sets, or {@code null} when it sets none
     */
    private static @Nullable String keyOf(@Nullable String reported, @Nullable ThemeColour colour) {
        return reported == null || colour == null ? null : colour.key();
    }
}
