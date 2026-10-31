package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * A style made alike a style of the same {@code .xlsx} workbook leaves the named styles of the workbook as they are:
 * the last one of them is no longer rewritten with the named style of the copied cell.
 */
class CopyStyleTest {

    @Test
    void copiesAStyleOfTheSameWorkbookByItsCellFormat() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = cellOfAHeading(workbook);
            var named = namedStylesOf(workbook.getStylesSource());

            var copy = (XSSFCellStyle) PoiExcelHelper.copyStyle(cell.getCellStyle(), workbook);

            assertNotEquals(cell.getCellStyle().getIndex(), copy.getIndex());
            assertEquals(((XSSFCellStyle) cell.getCellStyle()).getCoreXf().xmlText(), copy.getCoreXf().xmlText());
            assertEquals(named, namedStylesOf(workbook.getStylesSource()));
        }
    }

    @Test
    void clonesAStyleOfAnotherWorkbook() throws IOException {
        try (var from = new XSSFWorkbook(); var to = new XSSFWorkbook()) {
            var style = from.createCellStyle();
            var font = from.createFont();
            font.setBold(true);
            style.setFont(font);

            var copy = PoiExcelHelper.copyStyle(style, to);

            assertTrue(to.getFontAt(copy.getFontIndex()).getBold());
        }
    }

    @Test
    void leavesTheNamedStylesAsTheyAreWhenACellIsRestyled() throws IOException {
        assertNamedStylesKept(cell -> grid(cell).setCellFillColor(0, 0, new short[]{255, 0, 0}));
        assertNamedStylesKept(cell -> grid(cell).setCellFontColor(0, 0, new short[]{255, 0, 0}));
        assertNamedStylesKept(cell -> grid(cell).setCellBorderStyle(0, 0, borders()));
        assertNamedStylesKept(cell -> grid(cell).setCellStyle(0, 0, borders()));
        assertNamedStylesKept(cell -> grid(cell).setCellValue(0, 0, new Date(0)));
    }

    @Test
    void leavesTheNamedStylesAsTheyAreWhenANumberReplacesADate() throws IOException {
        assertNamedStylesKept(cell -> {
            cell.getCellStyle().setDataFormat((short) 14);
            grid(cell).setCellValue(0, 0, 5);
        });
    }

    private static void assertNamedStylesKept(Consumer<Cell> change) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = cellOfAHeading(workbook);
            var named = namedStylesOf(workbook.getStylesSource());

            change.accept(cell);

            assertEquals(named, namedStylesOf(workbook.getStylesSource()));
            assertEquals(1, ((XSSFCellStyle) cell.getCellStyle()).getCoreXf().getXfId(), "the cell keeps its named style");
        }
    }

    /** A cell of a style linked to the named style Heading, which is not the last named style of the workbook. */
    private static Cell cellOfAHeading(XSSFWorkbook workbook) {
        var styles = workbook.getStylesSource();
        var heading = CTXf.Factory.newInstance();
        heading.setFontId(0);
        heading.setNumFmtId(0);
        var headingId = styles.putCellStyleXf(heading) - 1;
        var percent = CTXf.Factory.newInstance();
        percent.setNumFmtId(10);
        styles.putCellStyleXf(percent);
        var xf = CTXf.Factory.newInstance();
        xf.setXfId(headingId);
        var cell = workbook.createSheet().createRow(0).createCell(0);
        cell.setCellStyle(workbook.getCellStyleAt(styles.putCellXf(xf) - 1));
        return cell;
    }

    private static List<String> namedStylesOf(StylesTable styles) {
        return IntStream.range(0, styles._getStyleXfsSize())
                .mapToObj(index -> styles.getCellStyleXfAt(index).xmlText())
                .toList();
    }

    /** A style that only draws a thin line around a cell. */
    private static ICellStyle borders() {
        var style = mock(ICellStyle.class);
        when(style.getBorderStyle())
                .thenReturn(new BorderStyle[]{BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN});
        return style;
    }

    private static XlsSheetGridModel grid(Cell cell) {
        var workbook = cell.getSheet().getWorkbook();
        var source = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule("./test/rules/XlsSheetGridModelTest.xls"),
                new SimpleWorkbookLoader(workbook));
        return new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, source));
    }
}
