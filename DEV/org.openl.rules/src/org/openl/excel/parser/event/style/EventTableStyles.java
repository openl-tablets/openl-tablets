package org.openl.excel.parser.event.style;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FontRecord;
import org.apache.poi.hssf.record.FormatRecord;
import org.apache.poi.hssf.record.PaletteRecord;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.openl.excel.parser.TableStyles;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsCellComment;

public class EventTableStyles implements TableStyles {
    private final IGridRegion region;
    private final int[][] cellIndexes;
    private final List<ExtendedFormatRecord> extendedFormats;
    private final Map<Integer, FormatRecord> customFormats;
    private final PaletteRecord palette;
    private final List<FontRecord> fonts;
    private final List<HSSFComment> comments;

    public EventTableStyles(IGridRegion region,
            int[][] cellIndexes,
            List<ExtendedFormatRecord> extendedFormats,
            Map<Integer, FormatRecord> customFormats,
            PaletteRecord palette,
            List<FontRecord> fonts,
            List<HSSFComment> comments) {
        this.region = region;
        this.cellIndexes = cellIndexes;
        this.extendedFormats = extendedFormats;
        this.customFormats = customFormats;
        this.palette = palette;
        this.fonts = fonts;
        this.comments = comments == null ? Collections.<HSSFComment>emptyList() : comments;
    }

    @Override
    public IGridRegion getRegion() {
        return region;
    }

    @Override
    public ICellStyle getStyle(int row, int column) {
        int index = cellIndexes[row - region.getTop()][column - region.getLeft()];
        return new OpenLCellStyle(index, extendedFormats.get(index), palette, customFormats);
    }

    @Override
    public ICellFont getFont(int row, int column) {
        int index = cellIndexes[row - region.getTop()][column - region.getLeft()];
        return new OpenLCellFont(getFont(extendedFormats.get(index).getFontIndex()), palette);
    }

    @Override
    public ICellComment getComment(int row, int column) {
        for (HSSFComment comment : comments) {
            if (comment.hasPosition() && comment.getRow() == row && comment.getColumn() == column) {
                return new XlsCellComment(comment);
            }
        }

        return null;
    }

    public FontRecord getFont(int index) {
        if (index > 4) {
            // Workaround for some strange behavior in HSSF format: there is no 4'th font.
            index -= 1;
        }

        return fonts.get(index);
    }

}
