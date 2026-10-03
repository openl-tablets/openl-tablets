package org.openl.studio.projects.service.tables.theme;

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
import org.openl.studio.projects.service.tables.read.RawTableStyles;

/**
 * Turns the look of the theme into the cell style the Tables API reports.
 *
 * <p>The style reported leaves out an attribute at its default, as the style read from the workbook does: a white
 * background, a black font, the left and bottom alignment, and a font that is not bold.
 *
 * <p>Every style the theme gives a cell, or a piece of its text, names the theme as its source
 * ({@link RawTableStyleSource#THEME}): a read naming the theme reports it in place of the style of the workbook.
 */
public final class ThemeStyles {

    private ThemeStyles() {
    }

    /**
     * Lays the look of the theme over a cell style.
     *
     * @param style the style the cell has in the workbook, or {@code null} when it has none
     * @param theme the look the theme gives the cell
     * @return the cell style with every attribute the theme sets replaced, the theme named as its source
     */
    public static RawTableCellStyle over(@Nullable RawTableCellStyle style, ThemeStyle theme) {
        var builder = (style == null ? RawTableCellStyle.builder() : style.toBuilder())
                .source(RawTableStyleSource.THEME);
        if (theme.background() != null) {
            builder.background(colour(theme.background(), RawTableStyles.WHITE));
        }
        if (theme.color() != null) {
            builder.color(colour(theme.color(), RawTableStyles.BLACK));
        }
        if (theme.align() != null) {
            builder.align(horizontal(theme.align()));
        }
        if (theme.valign() != null) {
            builder.valign(vertical(theme.valign()));
        }
        setFont(builder, theme);
        if (theme.border() != null) {
            builder.border(border(style == null ? null : style.border(), theme.border()));
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
    public static RawTableCellStyle fontOf(@Nullable RawTableCellStyle cell, ThemeStyle theme) {
        var builder = RawTableCellStyle.builder().source(RawTableStyleSource.THEME);
        if (cell != null) {
            builder.color(cell.color())
                    .bold(cell.bold())
                    .italic(cell.italic())
                    .underline(cell.underline())
                    .strikeout(cell.strikeout());
        }
        if (theme.color() != null) {
            builder.color(colour(theme.color(), RawTableStyles.BLACK));
        }
        setFont(builder, theme);
        return builder.build();
    }

    /** Sets the font attributes the theme names. */
    private static void setFont(RawTableCellStyle.RawTableCellStyleBuilder builder, ThemeStyle theme) {
        if (theme.bold() != null) {
            builder.bold(flag(theme.bold()));
        }
        if (theme.italic() != null) {
            builder.italic(flag(theme.italic()));
        }
        if (theme.underline() != null) {
            builder.underline(flag(theme.underline()));
        }
        if (theme.strikeout() != null) {
            builder.strikeout(flag(theme.strikeout()));
        }
        if (theme.fontFamily() != null) {
            builder.fontFamily(theme.fontFamily());
        }
        if (theme.fontSize() != null) {
            builder.fontSize(theme.fontSize());
        }
    }

    /** {@link Boolean#TRUE} for a flag that is on, {@code null} for one that is off or not named. */
    private static @Nullable Boolean flag(@Nullable Boolean value) {
        return Boolean.TRUE.equals(value) ? Boolean.TRUE : null;
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

    /** Sets one border side, drawn with the line the workbook draws it with once the theme is written. */
    private static void side(@Nullable ThemeBorderLine line,
                             Function<RawTableCellBorderSide, RawTableCellBorder.RawTableCellBorderBuilder> into) {
        if (line != null) {
            var drawn = BorderStyle.of(line.style().getExcel(), PoiExcelHelper.toRgb(line.colorOrBlack()));
            into.apply(RawTableStyles.borderSide(drawn));
        }
    }
}
