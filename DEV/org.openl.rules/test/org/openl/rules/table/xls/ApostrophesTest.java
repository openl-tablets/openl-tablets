package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

/**
 * An OpenL formula gets the apostrophe of Excel, and a value that is not a text loses it, each cell in a style made
 * once for the style it had.
 */
class ApostrophesTest {

    /** Enough styles for a lookup over all the styles of a workbook at each cell to show. */
    private static final int MANY_STYLES = 2_000;

    @Test
    void addsOneStyleForTheCellsOfOneStyle() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            for (var i = 0; i < MANY_STYLES; i++) {
                workbook.createCellStyle().setIndention((short) (i % 15));
            }
            var apostrophes = new Apostrophes();

            assertTimeout(Duration.ofSeconds(5), () -> {
                for (var row = 0; row < 1000; row++) {
                    var cell = sheet.createRow(row).createCell(0);
                    cell.setCellValue("= $F" + row + " * 2");
                    apostrophes.set(cell);
                }
            });

            assertEquals(MANY_STYLES + 2, workbook.getNumCellStyles(), "the default style, the many, and one more");
            assertTrue(sheet.getRow(999).getCell(0).getCellStyle().getQuotePrefixed());
            assertFalse(workbook.getCellStyleAt(0).getQuotePrefixed(), "the style the cells had stays as it is");
        }
    }

    @Test
    void takesTheApostropheFromAnExcelFormulaAndANumber() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            var quoted = workbook.createCellStyle();
            quoted.setQuotePrefixed(true);
            var formula = row.createCell(0);
            formula.setCellStyle(quoted);
            formula.setCellFormula("1+2");
            var number = row.createCell(1);
            number.setCellStyle(quoted);
            number.setCellValue(5);
            var typed = row.createCell(2);
            typed.setCellStyle(quoted);
            typed.setCellValue("0123");
            var apostrophes = new Apostrophes();

            apostrophes.set(formula);
            apostrophes.set(number);
            apostrophes.set(typed);

            assertFalse(formula.getCellStyle().getQuotePrefixed());
            assertEquals(formula.getCellStyle().getIndex(), number.getCellStyle().getIndex(), "made once");
            assertTrue(typed.getCellStyle().getQuotePrefixed(), "a text typed after an apostrophe stays one");
        }
    }

    /** A later write, of another grid model too, takes the style the workbook holds already. */
    @Test
    void takesTheStyleTheWorkbookHoldsAlready() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            var first = row.createCell(0);
            first.setCellValue("= 1");
            new Apostrophes().set(first);
            var styles = workbook.getNumCellStyles();
            var second = row.createCell(1);
            second.setCellValue("= 2");

            new Apostrophes().set(second);

            assertEquals(styles, workbook.getNumCellStyles());
            assertEquals(first.getCellStyle().getIndex(), second.getCellStyle().getIndex());
        }
    }

    /** A cell given an OpenL formula and then a number again takes back the style it had. */
    @Test
    void givesBackTheStyleACellHad() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var bold = workbook.createCellStyle();
            bold.setWrapText(true);
            cell.setCellStyle(bold);
            var apostrophes = new Apostrophes();
            cell.setCellValue("= 1");
            apostrophes.set(cell);
            var styles = workbook.getNumCellStyles();

            cell.setCellValue(1);
            apostrophes.set(cell);

            assertEquals(bold.getIndex(), cell.getCellStyle().getIndex());
            assertEquals(styles, workbook.getNumCellStyles());
        }
    }

    /** Excel would keep an Excel formula typed into an emptied cell after an apostrophe a text. */
    @Test
    void takesTheApostropheFromAnEmptiedCell() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var quoted = workbook.createCellStyle();
            quoted.setQuotePrefixed(true);
            cell.setCellStyle(quoted);

            new Apostrophes().set(cell);

            assertFalse(cell.getCellStyle().getQuotePrefixed());
        }
    }

    /** A new style copies the cell format alone: the named styles of the workbook stay as they are. */
    @Test
    void leavesTheNamedStylesOfTheWorkbookAsTheyAre() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var styles = workbook.getStylesSource();
            var heading = CTXf.Factory.newInstance();
            heading.setFontId(0);
            heading.setNumFmtId(0);
            var headingId = styles.putCellStyleXf(heading) - 1;
            var percent = CTXf.Factory.newInstance();
            percent.setNumFmtId(10);
            styles.putCellStyleXf(percent);
            var named = namedStylesOf(styles);
            var cellXf = CTXf.Factory.newInstance();
            cellXf.setXfId(headingId);
            var cell = workbook.createSheet().createRow(0).createCell(0);
            cell.setCellStyle(workbook.getCellStyleAt(styles.putCellXf(cellXf) - 1));
            cell.setCellValue("= 1");

            new Apostrophes().set(cell);

            assertTrue(cell.getCellStyle().getQuotePrefixed());
            assertEquals(headingId, cell.getCellStyle().getCoreXf().getXfId(), "the cell keeps its named style");
            assertEquals(named, namedStylesOf(styles));
        }
    }

    private static List<String> namedStylesOf(StylesTable styles) {
        return IntStream.range(0, styles._getStyleXfsSize())
                .mapToObj(index -> styles.getCellStyleXfAt(index).xmlText())
                .toList();
    }

    /** An .xlsx workbook holds up to 64,000 styles, past what a signed short counts. */
    @Test
    void takesAStyleOfAnIndexPastTheSignedShortOnes() {
        // Stub-only mocks keep no record of the calls a scan over 40,000 styles makes.
        var base = mock(CellStyle.class, withSettings().stubOnly());
        var twin = mock(CellStyle.class, withSettings().stubOnly());
        when(twin.getQuotePrefixed()).thenReturn(true);
        when(twin.getIndex()).thenReturn((short) 40_000);
        var workbook = mock(Workbook.class, withSettings().stubOnly());
        when(workbook.getNumCellStyles()).thenReturn(40_001);
        when(workbook.getCellStyleAt(anyInt())).thenAnswer(call -> {
            int index = call.getArgument(0);
            if (index < 0) {
                throw new IndexOutOfBoundsException(index);
            }
            return index == 40_000 ? twin : base;
        });
        var apostrophes = new Apostrophes();
        var first = formulaIn(base, workbook);
        var second = formulaIn(base, workbook);

        apostrophes.set(first);
        apostrophes.set(second);

        verify(first).setCellStyle(twin);
        verify(second).setCellStyle(twin);
    }

    private static Cell formulaIn(CellStyle style, Workbook workbook) {
        var cell = mock(Cell.class, RETURNS_DEEP_STUBS);
        when(cell.getCellType()).thenReturn(CellType.STRING);
        when(cell.getStringCellValue()).thenReturn("= 1");
        when(cell.getCellStyle()).thenReturn(style);
        when(cell.getSheet().getWorkbook()).thenReturn(workbook);
        return cell;
    }

    /** The styles of a workbook are renumbered when the unused ones are removed. */
    @Test
    void makesAStyleAgainWhenTheOneMadeBeforeChanged() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            var apostrophes = new Apostrophes();
            var first = row.createCell(0);
            first.setCellValue("= 1");
            apostrophes.set(first);
            var made = first.getCellStyle();
            made.setWrapText(true);
            var second = row.createCell(1);
            second.setCellValue("= 2");

            apostrophes.set(second);

            assertTrue(second.getCellStyle().getQuotePrefixed());
            assertFalse(second.getCellStyle().getWrapText());
            assertNotEquals(made.getIndex(), second.getCellStyle().getIndex());
        }
    }
}
