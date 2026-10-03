package org.openl.excel.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRst;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ui.TextRun;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.URLSourceCodeModule;

class TextRunsTest {

    private static final short[] GREY = {0x80, 0x80, 0x80};

    @TempDir
    Path folder;

    @Test
    void readsTheRunsOfASharedStringWithSax() throws IOException {
        var file = folder.resolve("runs.xlsx");
        writeWorkbook(file);

        try (var reader = ExcelReaderFactory.sequentialFactory().create(file.toString())) {
            var styles = reader.getTableStyles(reader.getSheets().getFirst(), new GridRegion(0, 0, 1, 0));

            assertHeaderRuns(styles.getTextRuns(0, 0));
            assertTrue(styles.getTextRuns(1, 0).isEmpty(), "A text with one font has no runs");
        }
    }

    @Test
    void readsTheRunsOfASharedStringStoredAfterTheStringsOfOtherTablesWithSax() throws IOException {
        var file = folder.resolve("later.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Model");
            // The strings of the table above come first among the shared strings of the workbook.
            sheet.createRow(0).createCell(0).setCellValue("Rules void hello()");
            sheet.createRow(1).createCell(0).setCellValue("C1");
            sheet.createRow(3).createCell(0).setCellValue(header(workbook));
            workbook.write(out);
        }

        try (var reader = ExcelReaderFactory.sequentialFactory().create(file.toString())) {
            var styles = reader.getTableStyles(reader.getSheets().getFirst(), new GridRegion(3, 0, 3, 1));

            assertHeaderRuns(styles.getTextRuns(3, 0));
        }
    }

    @Test
    void readsTheRunsOfACellOfAWorkbookHeldInMemory() throws IOException {
        var file = folder.resolve("held.xlsx");
        writeWorkbook(file);
        var source = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(file.toString()));
        var grid = new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, source));

        assertHeaderRuns(grid.getCell(0, 0).getTextRuns());
        // A cell the header is merged over answers with the runs of the cell holding the text.
        assertHeaderRuns(grid.getCell(1, 0).getTextRuns());
        assertTrue(grid.getCell(0, 1).getTextRuns().isEmpty(), "A text with one font has no runs");
        assertTrue(grid.getCell(3, 3).getTextRuns().isEmpty(), "A cell holding nothing has no runs");
    }

    @Test
    void readsTheRunsOfAnXlsxText() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            assertHeaderRuns(PoiExcelHelper.getTextRuns(header(workbook), workbook, null));
        }
    }

    @Test
    void readsTheRunsOfAnXlsText() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var bold = workbook.createFont();
            bold.setBold(true);
            var text = new HSSFRichTextString("Datatype Commission");
            text.applyFont(9, 19, bold);

            var runs = PoiExcelHelper.getTextRuns(text, workbook, null);

            assertEquals(2, runs.size());
            assertEquals("Datatype ", runs.get(0).text());
            assertNull(runs.get(0).font(), "A piece no run formats takes the cell font");
            assertEquals("Commission", runs.get(1).text());
            assertTrue(runs.get(1).font().isBold());
        }
    }

    @Test
    void readsTheRunOfATextFormattedWholeInAFontOfItsOwn() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var red = workbook.createFont();
            red.setColor(new XSSFColor(new byte[]{(byte) 0xff, 0, 0}));
            var text = new XSSFRichTextString("Premium");
            text.applyFont(red);

            var runs = PoiExcelHelper.getTextRuns(text, workbook, null);

            // The font is not the one of the cell, so the piece is read although it is the only one.
            assertEquals(1, runs.size());
            assertEquals("Premium", runs.getFirst().text());
            assertArrayEquals(new short[]{0xff, 0, 0}, runs.getFirst().font().getFontColor());
        }
    }

    @Test
    void cutsAnXlsxTextThatEscapesACharacterWhereItsRunsStart() {
        // Excel writes a carriage return, which XML cannot hold, as _x000D_; the text answers it unescaped.
        var written = CTRst.Factory.newInstance();
        written.addNewR().setT("line_x000D_");
        var bold = written.addNewR();
        bold.addNewRPr().addNewB();
        bold.setT("next_x0024_");

        var runs = PoiExcelHelper.getTextRuns(new XSSFRichTextString(written), null, null);

        assertEquals(List.of("line\r", "next$"), runs.stream().map(TextRun::text).toList());
        assertNull(runs.getFirst().font(), "A run that names no font takes the font of the cell");
        assertTrue(runs.getLast().font().isBold());
    }

    @Test
    void answersNoRunsForAPlainText() {
        assertTrue(PoiExcelHelper.getTextRuns(new XSSFRichTextString("plain"), null, null).isEmpty());
    }

    private static void assertHeaderRuns(List<TextRun> runs) {
        assertEquals(3, runs.size());
        assertEquals("Datatype", runs.get(0).text());
        assertArrayEquals(GREY, runs.get(0).font().getFontColor());
        assertFalse(runs.get(0).font().isBold());
        assertEquals(" ", runs.get(1).text());
        assertEquals("Commission", runs.get(2).text());
        assertTrue(runs.get(2).font().isBold());
    }

    private static void writeWorkbook(Path file) throws IOException {
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Model");
            sheet.createRow(0).createCell(0).setCellValue(header(workbook));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
            sheet.createRow(1).createCell(0).setCellValue("String name");
            workbook.write(out);
        }
    }

    private static XSSFRichTextString header(XSSFWorkbook workbook) {
        var grey = workbook.createFont();
        grey.setColor(new XSSFColor(new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80}));
        var plain = workbook.createFont();
        var bold = workbook.createFont();
        bold.setBold(true);
        var text = new XSSFRichTextString("Datatype Commission");
        text.applyFont(0, 8, grey);
        text.applyFont(8, 9, plain);
        text.applyFont(9, 19, bold);
        return text;
    }
}
