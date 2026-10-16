package org.openl.rules.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * Rows laid down one after another in a table of B2:D9, its header merged across, a comment in B6 and every row
 * filled in a colour of its own. Its body may also merge C3:C4, C8:D8 or both.
 *
 * @author Yury Molchan
 */
class GridToolTest {

    private static final GridRegion TABLE = new GridRegion(1, 1, 8, 3);
    private static final int ROWS = 3;

    @TempDir
    Path tempDir;

    /**
     * The sheet looks as single-row inserts leave it: under the header, in the middle and under the last row of a
     * table; under a merge, which grows over the rows; under a row merged across, which every row takes; across a
     * merge and above one, which take the rows one at a time.
     */
    @ParameterizedTest
    @CsvSource({"0, ''", "3, ''", "7, ''", "2, C3:C4", "6, C8:D8", "1, C3:C4", "0, C3:C4", "2, C3:C4 C8:D8"})
    void laysTheRowsDownAsSingleRowInsertsWould(int afterRow, String merges) throws IOException {
        var file = workbook(merges);
        var metaInfo = mock(MetaInfoWriter.class);
        var oneAfterAnother = grid(file);
        GridTool.insertRowsOneAfterAnother(ROWS, afterRow, TABLE, oneAfterAnother, metaInfo)
                .doAction(new GridTable(TABLE, oneAfterAnother));

        var singleRows = grid(file);
        for (var row = 0; row < ROWS; row++) {
            var grown = new GridRegion(TABLE.getTop(), TABLE.getLeft(), TABLE.getBottom() + row, TABLE.getRight());
            GridTool.insertRows(1, afterRow + row, grown, singleRows, metaInfo)
                    .doAction(new GridTable(grown, singleRows));
        }

        assertEquals(look(singleRows), look(oneAfterAnother));
    }

    @Test
    void movesTheRowsUnderTheBlockOnce() throws IOException {
        var grid = spy(grid(workbook("")));

        GridTool.insertRowsOneAfterAnother(ROWS, 2, TABLE, grid, mock(MetaInfoWriter.class))
                .doAction(new GridTable(TABLE, grid));

        // Rows 5 to 9 move, three cells each, every cell once with its comment. Laying the rows down one at a time
        // would move them three times.
        verify(grid, times(5 * 3)).setCellComment(anyInt(), anyInt(), any(), any());
    }

    private Path workbook(String merges) throws IOException {
        var file = tempDir.resolve("table.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet();
            var colours = IndexedColors.values();
            for (var row = TABLE.getTop(); row <= TABLE.getBottom(); row++) {
                var style = workbook.createCellStyle();
                style.setFillForegroundColor(colours[8 + row].getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                var sheetRow = sheet.createRow(row);
                for (var column = TABLE.getLeft(); column <= TABLE.getRight(); column++) {
                    var cell = sheetRow.createCell(column);
                    cell.setCellValue(row == TABLE.getTop() ? "Datatype T" : "r" + row + "c" + column);
                    cell.setCellStyle(style);
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 3));
            for (var merge : merges.split(" ")) {
                if (!merge.isEmpty()) {
                    sheet.addMergedRegion(CellRangeAddress.valueOf(merge));
                }
            }
            var anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
            var comment = sheet.createDrawingPatriarch().createCellComment(anchor);
            comment.setString(workbook.getCreationHelper().createRichTextString("note"));
            sheet.getRow(5).getCell(1).setCellComment(comment);
            workbook.write(out);
        }
        return file;
    }

    private static XlsSheetGridModel grid(Path file) {
        var workbook = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(file.toString()));
        return new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, workbook));
    }

    /**
     * Every row and cell of the sheet with its value, its look and its comment, then the merged regions.
     *
     * <p>A style is read by what it sets, not by its index: the two ways create the same looks in another order.
     */
    private static String look(XlsSheetGridModel grid) {
        var sheet = (XSSFSheet) grid.getSheetSource().getSheet();
        var text = new StringBuilder();
        for (var row : sheet) {
            text.append("row ").append(row.getRowNum()).append(' ').append(row.getHeight()).append('\n');
            for (var cell : row) {
                text.append(look((XSSFCell) cell)).append('\n');
            }
        }
        return text.append(sheet.getMergedRegions()
                        .stream()
                        .map(CellRangeAddress::formatAsString)
                        .sorted()
                        .collect(Collectors.joining(" ")))
                .toString();
    }

    private static String look(XSSFCell cell) {
        var style = (XSSFCellStyle) cell.getCellStyle();
        var styles = cell.getSheet().getWorkbook().getStylesSource();
        var xf = style.getCoreXf();
        var comment = cell.getCellComment();
        return String.join(" | ",
                cell.getAddress().formatAsString(),
                cell.getCellType().name(),
                cell.toString(),
                style.getDataFormatString(),
                style.getFont().getCTFont().xmlText(),
                styles.getFillAt((int) xf.getFillId()).getCTFill().xmlText(),
                styles.getBorderAt((int) xf.getBorderId()).getCTBorder().xmlText(),
                xf.isSetAlignment() ? xf.getAlignment().xmlText() : "",
                comment == null ? "" : comment.getAuthor() + ": " + comment.getString().getString());
    }
}
