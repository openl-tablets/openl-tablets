package org.openl.rules.table.xls.builder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsCellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * A table written into a workbook other than the one it was read from keeps the colours it is seen with,
 * which is what copying a table into another module does.
 */
class TableBuilderStyleTest {

    private static final short FILL = 21;
    private static final short[] PALE_BLUE = {0xdc, 0xe6, 0xf2};
    private static final short[] ORANGE = {0x4a, 0x45, 0x2a};

    /**
     * A workbook holding one filled cell, whose palette gives {@link #FILL} the colour above. The default
     * palette gives that number another colour, so the number alone does not carry it.
     */
    private static Path sourceWorkbook(Path folder) throws IOException {
        var path = folder.resolve("source.xlsx");
        try (var workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(path)) {
            var palette = workbook.getStylesSource().getCTStylesheet().addNewColors().addNewIndexedColors();
            for (var i = 0; i < 64; i++) {
                var colour = i == FILL ? PALE_BLUE : new short[]{0, 0, 0};
                palette.addNewRgbColor().setRgb(new byte[]{(byte) colour[0], (byte) colour[1], (byte) colour[2]});
            }
            var style = workbook.createCellStyle();
            style.setFillForegroundColor(FILL);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            var cell = workbook.createSheet("Source").createRow(0).createCell(0);
            cell.setCellValue("Rate");
            cell.setCellStyle(style);
            workbook.write(out);
        }
        return path;
    }

    private static Path emptyWorkbook(Path folder) throws IOException {
        var path = folder.resolve("destination.xlsx");
        try (var workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(path)) {
            workbook.createSheet("Destination");
            workbook.write(out);
        }
        return path;
    }

    private static XlsSheetGridModel gridOf(Path workbook) {
        var source = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(workbook.toString()));
        return new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, source));
    }

    /**
     * A colour set on a cell is written as a colour a workbook can hold.
     *
     * <p>A colour is a value of four bytes, and a spreadsheet application reads a workbook holding three
     * bytes of one as damaged content — whether the colour was carried with a copied table or set by hand.
     */
    @Test
    void aColourSetOnACellIsOneTheWorkbookCanHold(@TempDir Path folder) throws IOException {
        var grid = gridOf(emptyWorkbook(folder));
        grid.setCellFillColor(0, 0, PALE_BLUE);
        grid.setCellFontColor(0, 0, ORANGE);

        var workbook = grid.getSheetSource().getSheet().getWorkbook();
        assertEquals(List.of(), WrittenWorkbook.coloursShortOfAValue(WrittenWorkbook.stylesOf(workbook)));
        var style = workbook.getCellStyleAt(workbook.getNumCellStyles() - 1);
        assertArrayEquals(PALE_BLUE, new XlsCellStyle(style, workbook).getFillForegroundColor());
        assertArrayEquals(ORANGE, PoiExcelHelper.getFontColor(workbook.getFontAt(style.getFontIndex()), workbook));
    }

    @Test
    void aTableWrittenIntoAnotherWorkbookKeepsTheColoursItIsSeenWith(@TempDir Path folder)
            throws IOException, CreateTableException {
        var source = gridOf(sourceWorkbook(folder));
        var destination = gridOf(emptyWorkbook(folder));
        var seenInSource = source.getCell(0, 0).getStyle().getFillForegroundColor();
        assertArrayEquals(PALE_BLUE, seenInSource, "the source cell as its own workbook shows it");

        var builder = new TableBuilder(destination);
        builder.beginTable(1, 1);
        builder.writeGridTable(new GridTable(0, 0, 0, 0, source));
        var region = builder.getTableRegion();
        builder.endTable();

        var written = destination.getCell(region.getLeft(), region.getTop());
        assertEquals("Rate", written.getStringValue());
        assertArrayEquals(PALE_BLUE, written.getStyle().getFillForegroundColor(), "the colour it was written");
    }
}
