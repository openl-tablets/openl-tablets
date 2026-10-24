package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.jspecify.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.studio.projects.model.tables.RawTextMark;
import org.openl.studio.projects.model.tables.RawTextMarkColor;

/**
 * The marks on the pieces of a cell text: each piece is bold in the standard colour of Excel of its mark.
 *
 * <p>A mark is written as rich text of the cell. The font of a marked piece is kept in the text itself, so a mark adds
 * no cell style, cell format, named style or font to the styles of the workbook.
 *
 * <p>A piece of any other font, such as one coloured or made italic in Excel, is not a mark.
 *
 * @author Yury Molchan
 */
public final class TextMarks {

    private TextMarks() {
    }

    /**
     * The marks of a cell text, in the order of the text, each mark joined with the next one of its colour.
     *
     * @return the marks, or an empty list for a cell that holds no text or no marked piece
     */
    public static List<RawTextMark> of(XSSFCell cell) {
        if (cell.getCellType() != CellType.STRING) {
            return List.of();
        }
        return of(cell.getRichStringCellValue(), cell.getSheet().getWorkbook());
    }

    /**
     * The marks of a text of a workbook, in the order of the text, each mark joined with the next one of its colour.
     *
     * @return the marks, or an empty list for a text with no marked piece
     */
    public static List<RawTextMark> of(XSSFRichTextString rich, Workbook workbook) {
        if (rich.numFormattingRuns() == 0) {
            return List.of();
        }
        var marks = new ArrayList<RawTextMark>();
        var from = 0;
        for (var run : PoiExcelHelper.getTextRuns(rich, workbook, null)) {
            var to = from + run.text().length();
            var color = Optional.ofNullable(run.font()).map(TextMarks::colorOf).orElse(null);
            if (color != null) {
                marks.add(new RawTextMark(from, to, color));
            }
            from = to;
        }
        return joined(marks);
    }

    /** Joins each mark with the next one when it is of the same colour and starts where it ends. */
    public static List<RawTextMark> joined(List<RawTextMark> marks) {
        var joined = new ArrayList<RawTextMark>(marks.size());
        for (var mark : marks) {
            var last = joined.isEmpty() ? null : joined.getLast();
            if (last != null && last.color() == mark.color() && last.to().equals(mark.from())) {
                joined.set(joined.size() - 1, new RawTextMark(last.from(), mark.to(), mark.color()));
            } else {
                joined.add(mark);
            }
        }
        return List.copyOf(joined);
    }

    /**
     * Whether marks fit a text: each covers some of it, they follow the order of the text and none overlaps another.
     */
    public static boolean fit(List<RawTextMark> marks, String text) {
        var from = 0;
        for (var mark : marks) {
            if (mark.from() < from || mark.to() <= mark.from() || mark.to() > text.length()) {
                return false;
            }
            from = mark.to();
        }
        return true;
    }

    /**
     * A text with its marked pieces in the fonts of their marks. The rest of the text takes the font of its cell, and a
     * text with no marks is plain.
     *
     * @param fontOf the font of a mark
     */
    public static XSSFRichTextString write(String text, List<RawTextMark> marks,
            Function<RawTextMarkColor, XSSFFont> fontOf) {
        if (marks.isEmpty()) {
            // A text with no marks holds no run at all, so the formatter reads it as plain.
            return new XSSFRichTextString(text);
        }
        // One run appended per piece: a font applied to a range rebuilds every run of the text.
        var rich = new XSSFRichTextString();
        var from = 0;
        for (var mark : marks) {
            if (mark.from() > from) {
                rich.append(text.substring(from, mark.from()));
            }
            rich.append(text.substring(mark.from(), mark.to()), fontOf.apply(mark.color()));
            from = mark.to();
        }
        if (from < text.length()) {
            rich.append(text.substring(from));
        }
        return rich;
    }

    /** The font of a mark: the font given, bold, in the colour of the mark. It is not added to the workbook. */
    public static XSSFFont fontOf(CTFont font, RawTextMarkColor color) {
        var marked = new XSSFFont((CTFont) font.copy());
        marked.setBold(true);
        // Replaced whole: a theme colour of the font would win over the RGB XSSFFont.setColor writes beside it.
        var rgb = color.getRgb();
        var colour = CTColor.Factory.newInstance();
        colour.setRgb(new byte[]{(byte) 0xFF, (byte) (rgb >> 16), (byte) (rgb >> 8), (byte) rgb});
        marked.getCTFont().setColorArray(new CTColor[]{colour});
        return marked;
    }

    /**
     * Writes a text into a cell. A text written into the cell itself, as tools other than Excel write it, would
     * keep its characters and lose its pieces: blanking the cell first makes the text a shared string.
     */
    public static void writeText(XSSFCell cell, XSSFRichTextString text) {
        if (cell.getCTCell().getT() == STCellType.INLINE_STR) {
            cell.setBlank();
        }
        cell.setCellValue(text);
    }

    /** The colour of the mark a font draws, or {@code null} when the font draws no mark. */
    private static @Nullable RawTextMarkColor colorOf(ICellFont font) {
        var rgb = font.isBold() ? font.getFontColor() : null;
        if (rgb == null) {
            return null;
        }
        var value = PoiExcelHelper.toRgbValue(rgb);
        for (var color : RawTextMarkColor.values()) {
            if (color.getRgb() == value) {
                return color;
            }
        }
        return null;
    }
}
