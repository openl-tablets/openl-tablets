package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.helpers.ArraySplitter;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.xls.formatters.FormatConstants;
import org.openl.rules.table.xls.writers.XlsCellArrayWriter;
import org.openl.source.impl.URLSourceCodeModule;

class XlsSheetGridModelTest {

    private static XlsSheetGridModel xsGrid;

    @BeforeEach
    void before() {
        var source = new URLSourceCodeModule("./test/rules/XlsSheetGridModelTest.xls");
        var wbSrc = new XlsWorkbookSourceCodeModule(source);

        var sheetSrc = new XlsSheetSourceCodeModule(0, wbSrc);

        xsGrid = new XlsSheetGridModel(sheetSrc);
    }

    @Test
    void testConversions() {
        _testCell("A1", 0, 0);
        _testCell("AA1", 26, 0);
        _testCell("AB1", 27, 0);
    }

    private void _testCell(String cell, int col, int row) {
        assertEquals(IGridRegion.Tool.getColumn(cell), col);
        assertEquals(IGridRegion.Tool.getRow(cell), row);

        assertEquals(cell, XlsUtil.xlsCellPresentation(col, row));
    }

    @Test
    void testCellsFromMergedRegions() {
        var cell = xsGrid.getCell(2, 2);
        assertEquals("Rules void hello1(int hour)", cell.getStringValue());

        var cell1 = xsGrid.getCell(4, 2);
        assertEquals("Rules void hello1(int hour)", cell1.getStringValue());

        var cell2 = xsGrid.getCell(2, 8);
        assertEquals("R20", cell2.getStringValue());

        var cell3 = xsGrid.getCell(2, 9);
        assertEquals("R20", cell3.getStringValue());
    }

    @Test
    void testMergedRegionsNumber() {
        var mergedRegions = xsGrid.getNumberOfMergedRegions();
        assertEquals(13, mergedRegions);
    }

    @Test
    void testColumnIndexes() {
        var maxColumnIndex = xsGrid.getMaxColumnIndex(2);
        assertEquals(8, maxColumnIndex);

        var minColumnIndex = xsGrid.getMinColumnIndex(2);
        assertEquals(1, minColumnIndex);
    }

    @Test
    void testGetRegion() {
        assertTrue(xsGrid.isPartOfTheMergedRegion(3, 2));
        var gridRegion = xsGrid.getRegionContaining(3, 2);

        assertEquals(2, gridRegion.getTop());
        assertEquals(2, gridRegion.getBottom());

        assertEquals(2, gridRegion.getLeft());
        assertEquals(7, gridRegion.getRight());
    }

    @Test
    void testIsEmpty() {
        assertTrue(xsGrid.isEmpty(4, 12)); // trully empty cell

        assertTrue(xsGrid.isEmpty(4, 11)); // second cell from the merged region.
        // is it right behaviour?

        assertFalse(xsGrid.isEmpty(3, 11));// trully not emty cell.
    }

    @Test
    void writesArrayElementsAsParseableText() {
        xsGrid.setCellValue(0, 0, new String[]{"ACME, Inc", "C:\\", "Other"});

        var cellValue = (String) xsGrid.getCell(0, 0).getObjectValue();
        assertEquals("ACME\\, Inc,C:\\ ,Other", cellValue);
        assertArrayEquals(new String[]{"ACME, Inc", "C:\\", "Other"}, ArraySplitter.split(cellValue));
    }

    @Test
    void preservesNullPositionsInEnumArrays() {
        xsGrid.setCellValue(0, 0, new CountriesEnum[]{CountriesEnum.US, null, CountriesEnum.UA});
        assertEquals("US,,UA", xsGrid.getCell(0, 0).getObjectValue());

        xsGrid.setCellValue(0, 0, new CountriesEnum[]{null, null});
        assertEquals(",", xsGrid.getCell(0, 0).getObjectValue());
    }

    @Test
    void writesDateArrayElementsUsingCellFormat() {
        var sheet = xsGrid.getSheetSource().getSheet();
        var cell = PoiExcelHelper.getOrCreateCell(0, 0, sheet);
        var style = sheet.getWorkbook().createCellStyle();
        style.setDataFormat((short) BuiltinFormats.getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT));
        cell.setCellStyle(style);

        xsGrid.setCellValue(0, 0, new Date[]{date(2024, Calendar.JANUARY, 2), date(2024, Calendar.FEBRUARY, 3)});

        assertEquals("1/2/24,2/3/24", xsGrid.getCell(0, 0).getObjectValue());
    }

    @Test
    void writesDateArrayElementsUsing1904Windowing() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            workbook.getCTWorkbook().getWorkbookPr().setDate1904(true);
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat("[h]"));
            cell.setCellStyle(style);
            var writer = new XlsCellArrayWriter(xsGrid);
            writer.setCellToWrite(cell);
            writer.setValueToWrite(new Date[]{date(1904, Calendar.JANUARY, 2)});

            writer.writeCellValue();

            assertEquals("24", cell.getStringCellValue());
        }
    }

    @Test
    void writesTextualDateArrayElementsUsingDefaultLocale() {
        var defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            var sheet = xsGrid.getSheetSource().getSheet();
            var cell = PoiExcelHelper.getOrCreateCell(0, 0, sheet);
            var style = sheet.getWorkbook().createCellStyle();
            style.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("mmm d yyyy"));
            cell.setCellStyle(style);
            var value = date(2024, Calendar.JANUARY, 2);

            xsGrid.setCellValue(0, 0, new Date[]{value});

            var expected = DateTimeFormatter.ofPattern("MMM d yyyy", Locale.GERMANY)
                    .format(value.toInstant().atZone(ZoneId.systemDefault()));
            assertEquals(expected, xsGrid.getCell(0, 0).getObjectValue());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    void writesDateArrayElementsLosslesslyForGeneralCells() {
        xsGrid.setCellValue(0, 0, new Date[]{date(2050, Calendar.JANUARY, 2, 3, 4, 5, 678)});

        assertEquals("2050-01-02T03:04:05.678", xsGrid.getCell(0, 0).getObjectValue());
    }

    @Test
    void serializesArraysWithoutCellMetadata() {
        assertEquals("ACME\\, Inc,", XlsCellArrayWriter.serialize(new Object[]{"ACME, Inc", null}));
    }

    private static Date date(int year, int month, int day) {
        return new GregorianCalendar(year, month, day).getTime();
    }

    private static Date date(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        var calendar = new GregorianCalendar(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, millisecond);
        return calendar.getTime();
    }

}
