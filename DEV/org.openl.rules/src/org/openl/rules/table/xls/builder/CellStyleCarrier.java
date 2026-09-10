package org.openl.rules.table.xls.builder;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.ReadingOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsCellStyle;

/**
 * Makes a cell style of one workbook available in another, keeping the way it looks.
 *
 * <p>A colour can be held as a number that only the workbook it was written in gives meaning to, through a
 * palette of its own. The number survives the copy but the palette does not, so a carried style is given the
 * colours the source shows: the fill, the four borders and the font.
 *
 * <p>A style of the destination workbook that already looks the same is used instead of a new one, so
 * carrying the same style again costs the workbook nothing.
 */
final class CellStyleCarrier {

    private final Workbook destination;
    private final Map<Font, Font> fonts = new IdentityHashMap<>();

    CellStyleCarrier(Workbook destination) {
        this.destination = destination;
    }

    /**
     * A style of the destination workbook that looks like the given one.
     */
    CellStyle carry(XlsCellStyle source) {
        var wanted = new Appearance(source);
        var lookalike = findLookalike(source.getXlsStyle(), wanted);
        if (lookalike != null) {
            return lookalike;
        }
        var carried = PoiExcelHelper.<CellStyle> createCellStyle(destination);
        layOut(carried, source);
        paint(carried, wanted);
        return carried;
    }

    /**
     * Gives the carried style the layout of the source: its alignment, its borders and its number format.
     *
     * <p>Each is set on a style this workbook made, rather than copied as the source workbook wrote it. A
     * copied one is written back under the namespace of the workbook it came from, which the spreadsheet
     * application reads as damaged content, and a copy is refused between workbooks of different formats.
     */
    private void layOut(CellStyle carried, XlsCellStyle source) {
        var style = source.getXlsStyle();
        carried.setAlignment(style.getAlignment());
        carried.setVerticalAlignment(style.getVerticalAlignment());
        carried.setWrapText(style.getWrapText());
        carried.setShrinkToFit(style.getShrinkToFit());
        carried.setIndention((short) style.getIndention());
        carried.setRotation(style.getRotation());
        carried.setHidden(style.getHidden());
        carried.setLocked(style.getLocked());
        carried.setQuotePrefixed(style.getQuotePrefixed());
        carried.setBorderTop(style.getBorderTop());
        carried.setBorderRight(style.getBorderRight());
        carried.setBorderBottom(style.getBorderBottom());
        carried.setBorderLeft(style.getBorderLeft());
        carried.setFillPattern(style.getFillPattern());
        if (carried instanceof XSSFCellStyle target) {
            target.setReadingOrder(readingOrderOf(style));
        }
        // A number format is named by an index of its own workbook, so it is carried by what it spells out.
        // A workbook that spells out none for the style leaves the carried one with the general format.
        var format = source.getFormatString();
        if (format != null) {
            carried.setDataFormat(destination.createDataFormat().getFormat(format));
        }
    }

    /**
     * Writes the colours the source shows onto the carried style, replacing the ones its numbers stand for
     * in the destination workbook.
     */
    private void paint(CellStyle carried, Appearance wanted) {
        PoiExcelHelper.setCellFillColors(carried, wanted.fill, wanted.background, destination);
        PoiExcelHelper.setCellBorderColors(carried, wanted.borders, destination);
        carried.setFont(fonts.computeIfAbsent(wanted.font, font -> copyOf(font, wanted.fontColour)));
    }

    /**
     * A font of the destination workbook that looks like the given one.
     *
     * <p>Fonts are shared between the styles that name the same one, so a table written in one font adds one.
     */
    private Font copyOf(Font source, short[] colour) {
        var font = destination.createFont();
        font.setFontName(source.getFontName());
        // In twips, so a size of half a point is kept.
        font.setFontHeight(source.getFontHeight());
        font.setBold(source.getBold());
        font.setItalic(source.getItalic());
        font.setStrikeout(source.getStrikeout());
        font.setUnderline(source.getUnderline());
        font.setTypeOffset(source.getTypeOffset());
        font.setCharSet(source.getCharSet());
        PoiExcelHelper.setFontColor(font, colour, destination);
        return font;
    }

    private CellStyle findLookalike(CellStyle source, Appearance wanted) {
        for (var i = 0; i < destination.getNumCellStyles(); i++) {
            var candidate = destination.getCellStyleAt(i);
            if (sameLayout(candidate, source) && wanted.looksLike(new Appearance(candidate, destination))) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Whether the two styles lay a cell out the same way, colours aside.
     *
     * <p>The number format is compared by what it spells out: its index is one of its own workbook.
     */
    private static boolean sameLayout(CellStyle one, CellStyle other) {
        return one.getAlignment() == other.getAlignment() && one.getHidden() == other.getHidden() && one
                .getLocked() == other.getLocked() && one.getWrapText() == other.getWrapText() && one
                .getShrinkToFit() == other.getShrinkToFit() && one.getQuotePrefixed() == other.getQuotePrefixed() && one
                .getBorderBottom() == other.getBorderBottom() && one.getBorderLeft() == other.getBorderLeft() && one
                .getBorderRight() == other.getBorderRight() && one.getBorderTop() == other.getBorderTop() && one
                .getFillPattern() == other.getFillPattern() && one.getIndention() == other.getIndention() && one
                .getRotation() == other.getRotation() && one
                .getVerticalAlignment() == other.getVerticalAlignment() && readingOrderOf(one) == readingOrderOf(
                        other) && Objects.equals(one.getDataFormatString(), other.getDataFormatString());
    }

    /**
     * The direction a style lays its text out in.
     *
     * <p>Only the newer format records one, and a workbook of the older format lays text out the way the
     * reader does.
     */
    private static ReadingOrder readingOrderOf(CellStyle style) {
        return style instanceof XSSFCellStyle xssf ? xssf.getReadingOrder() : ReadingOrder.CONTEXT;
    }

    /**
     * What a style looks like: the colours it shows and the font it is written in.
     *
     * <p>The colours are the ones seen, not the numbers standing for them — the same number means a
     * different colour in each workbook — so two of these compare as the styles behind them look.
     */
    private static final class Appearance {

        private final short[] fill;
        private final short[] background;
        private final short[][] borders;
        private final Font font;
        private final short[] fontColour;

        Appearance(XlsCellStyle style) {
            fill = style.getFillForegroundColor();
            background = style.getFillBackgroundColor();
            borders = style.getBorderRGB();
            var workbook = style.getWorkbook();
            font = workbook.getFontAt(style.getXlsStyle().getFontIndex());
            fontColour = PoiExcelHelper.getFontColor(font, workbook);
        }

        Appearance(CellStyle style, Workbook workbook) {
            this(new XlsCellStyle(style, workbook));
        }

        boolean looksLike(Appearance other) {
            return Arrays.equals(fill, other.fill) && Arrays.equals(background, other.background) && Arrays
                    .deepEquals(borders, other.borders) && Arrays.equals(fontColour, other.fontColour) && sameFont(font,
                            other.font);
        }

        private static boolean sameFont(Font one, Font other) {
            return one.getFontName().equals(other.getFontName()) && one.getFontHeight() == other
                    .getFontHeight() && one.getBold() == other.getBold() && one.getItalic() == other.getItalic() && one
                    .getStrikeout() == other.getStrikeout() && one.getUnderline() == other.getUnderline() && one
                    .getTypeOffset() == other.getTypeOffset() && one.getCharSet() == other.getCharSet();
        }
    }
}
