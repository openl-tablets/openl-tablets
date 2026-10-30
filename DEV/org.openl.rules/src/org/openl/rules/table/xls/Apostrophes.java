package org.openl.rules.table.xls;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellPropertyType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

import org.openl.rules.binding.RuleRowHelper;

/**
 * Sets the apostrophe of Excel, the quote prefix of a cell style, by the value a cell holds.
 *
 * <p>A text starting with {@code =} is an OpenL formula and gets the apostrophe, so that Excel keeps it a text when
 * the cell is edited in Excel. A value that is not a text, such as an Excel formula or a number, loses it, as Excel
 * would turn it into a text once edited, and so does an emptied cell. Any other text keeps the cell as it is: a text
 * a user typed after an apostrophe, such as {@code 0123}, stays one.
 *
 * <p>A cell whose apostrophe changes takes the style alike the one it had but for the apostrophe. A style the workbook
 * holds already is taken, and one is added only when the workbook has none, so the other cells of the style the cell
 * had keep theirs and writing many cells adds at most one style for each style they had.
 */
@NullMarked
public final class Apostrophes {

    /** What becomes of the apostrophe of a cell. */
    public enum Quote {
        /** Kept as the cell has it: a text other than an OpenL formula. */
        KEEP,
        /** Set: an OpenL formula. */
        SET,
        /**
         * Dropped: anything but a text, an empty cell too, since Excel would turn an Excel formula or a number typed
         * there into a text.
         */
        DROP;

        /**
         * What becomes of the apostrophe of a cell.
         *
         * @param text the text of the cell, or {@code null} when it holds anything but a text
         * @return what becomes of its apostrophe
         */
        public static Quote of(@Nullable String text) {
            if (text == null) {
                return DROP;
            }
            return RuleRowHelper.isFormula(text) ? SET : KEEP;
        }
    }

    /** The style taken for a style, by their indexes, both ways. */
    private final Map<Integer, Integer> twins = new HashMap<>();

    /**
     * Sets the apostrophe of a cell by the value it holds.
     *
     * @param cell the cell
     */
    public void set(Cell cell) {
        var type = cell.getCellType();
        var quote = Quote.of(type == CellType.STRING ? cell.getStringCellValue() : null);
        if (quote == Quote.KEEP) {
            return;
        }
        var quoted = quote == Quote.SET;
        var style = cell.getCellStyle();
        if (style.getQuotePrefixed() != quoted) {
            cell.setCellStyle(twinOf(style, quoted, cell.getSheet().getWorkbook()));
        }
    }

    /** The style alike the given one but for the apostrophe. */
    private CellStyle twinOf(CellStyle style, boolean quoted, Workbook workbook) {
        var content = contentOf(style);
        var known = twins.get(indexOf(style));
        // The styles of a workbook are renumbered when the unused ones are removed: a style taken before is checked.
        if (known != null && known < workbook.getNumCellStyles()) {
            var twin = workbook.getCellStyleAt(known);
            if (twin.getQuotePrefixed() == quoted && content.equals(contentOf(twin))) {
                return twin;
            }
        }
        var twin = find(style, content, quoted, workbook);
        twins.put(indexOf(style), indexOf(twin));
        twins.put(indexOf(twin), indexOf(style));
        return twin;
    }

    /** The index of a style: an {@code .xlsx} workbook holds more styles than a signed {@code short} counts. */
    private static int indexOf(CellStyle style) {
        return Short.toUnsignedInt(style.getIndex());
    }

    /** The style the workbook holds alike the given one but for the apostrophe, or a new one. */
    private static CellStyle find(CellStyle style, Object content, boolean quoted, Workbook workbook) {
        for (var index = 0; index < workbook.getNumCellStyles(); index++) {
            var candidate = workbook.getCellStyleAt(index);
            if (isTwin(candidate, style, quoted) && content.equals(contentOf(candidate))) {
                return candidate;
            }
        }
        return copyOf(style, quoted, workbook);
    }

    /** A new style alike the given one but for the apostrophe. */
    private static CellStyle copyOf(CellStyle style, boolean quoted, Workbook workbook) {
        if (style instanceof XSSFCellStyle xssf && workbook instanceof XSSFWorkbook xlsx) {
            // POI's cloneStyleFrom rewrites the last named style of an .xlsx workbook too: the cell format is copied.
            var xf = (CTXf) xssf.getCoreXf().copy();
            if (quoted) {
                xf.setQuotePrefix(true);
            } else if (xf.isSetQuotePrefix()) {
                xf.unsetQuotePrefix();
            }
            return xlsx.getCellStyleAt(xlsx.getStylesSource().putCellXf(xf) - 1);
        }
        var copy = workbook.createCellStyle();
        copy.cloneStyleFrom(style);
        copy.setQuotePrefixed(quoted);
        return copy;
    }

    /** Whether a style may be the twin of another: told by what is cheap to read, before their whole content. */
    private static boolean isTwin(CellStyle candidate, CellStyle style, boolean quoted) {
        return candidate.getQuotePrefixed() == quoted && candidate.getFontIndex() == style.getFontIndex() && candidate
                .getDataFormat() == style.getDataFormat() && candidate.getFillPattern() == style.getFillPattern();
    }

    /** All a style holds but its apostrophe: the whole cell format of an {@code .xlsx} workbook. */
    private static Object contentOf(CellStyle style) {
        if (style instanceof XSSFCellStyle xssf) {
            var xf = (CTXf) xssf.getCoreXf().copy();
            if (xf.isSetQuotePrefix()) {
                xf.unsetQuotePrefix();
            }
            return xf.xmlText();
        }
        // The colours of an .xls workbook are indexed, which the properties hold.
        var properties = CellUtil.getFormatProperties(style);
        properties.remove(CellPropertyType.QUOTE_PREFIXED);
        return properties;
    }
}
