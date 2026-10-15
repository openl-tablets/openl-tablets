package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyle;

/**
 * Covers the removal of the cell formats and the named styles a workbook no longer uses, which formatting the Excel
 * files of a project runs on each workbook it saves.
 *
 * @author Yury Molchan
 */
class UnusedStylesTest {

    @Test
    void removesTheNamedStylesNoCellFormatLinks() throws IOException {
        try (var in = Files.newInputStream(Path.of("test-resources/unused-styles.xlsx"));
             var workbook = new XSSFWorkbook(in)) {
            assertEquals(3, workbook.getStylesSource().getCTStylesheet().getCellStyles().sizeOfCellStyleArray());

            ExcelTableFormatter.removeUnusedStyles(workbook);

            try (var saved = reread(workbook)) {
                var styles = saved.getStylesSource();
                var named = styles.getCTStylesheet().getCellStyles().getCellStyleList().stream()
                        .map(CTCellStyle::getName)
                        .toList();
                assertEquals(2, named.size());
                assertTrue(named.contains("Normal"));
                assertTrue(named.contains("My Custom Style 1"), "The style a cell uses stays");
                assertEquals(styles.getCTStylesheet().getCellStyles().sizeOfCellStyleArray(),
                        styles.getCTStylesheet().getCellStyles().getCount());
                assertEquals(3, styles._getStyleXfsSize(), "The cell styles the cell formats link");
            }
        }
    }

    @Test
    void removesTheCellFormatsNoCellUsesAndKeepsWhatEachCellLooksLike() throws IOException {
        // Read from a file, as a project workbook is: its cell formats are the ones of the stylesheet.
        try (var workbook = reread(yellowCells())) {
            assertEquals(3, workbook.getNumCellStyles());

            ExcelTableFormatter.removeUnusedStyles(workbook);

            try (var saved = reread(workbook)) {
                assertEquals(2, saved.getNumCellStyles(), "The first cell format and the yellow one");
                var written = saved.getSheet("Module");
                assertEquals(IndexedColors.YELLOW.getIndex(),
                        written.getRow(0).getCell(0).getCellStyle().getFillForegroundColor());
                assertEquals(IndexedColors.YELLOW.getIndex(), written.getRow(1).getRowStyle().getFillForegroundColor());
                assertEquals(IndexedColors.YELLOW.getIndex(), written.getColumnStyle(2).getFillForegroundColor());
            }
        }
    }

    /** A workbook with a cell, a row and a column in yellow, and a red cell format no cell uses before the yellow one. */
    private static XSSFWorkbook yellowCells() {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Module");
        var unused = workbook.createCellStyle();
        unused.setFillForegroundColor(IndexedColors.RED.getIndex());
        unused.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        var yellow = workbook.createCellStyle();
        yellow.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        yellow.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        var cell = sheet.createRow(0).createCell(0);
        cell.setCellValue("Rate");
        cell.setCellStyle(yellow);
        sheet.createRow(1).setRowStyle(yellow);
        sheet.setDefaultColumnStyle(2, yellow);
        return workbook;
    }

    /**
     * A workbook that names none of its styles is left carrying no list of them, and stays a workbook a spreadsheet
     * application opens: an empty list is content such an application reads as damaged.
     */
    @Test
    void leavesAWorkbookNamingNoStyleWithoutTheList() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            workbook.createSheet("Module").createRow(0).createCell(0).setCellValue("Rate");
            assertNull(workbook.getStylesSource().getCTStylesheet().getCellStyles(), "no list to begin with");

            ExcelTableFormatter.removeUnusedStyles(workbook);

            try (var written = reread(workbook)) {
                assertNull(written.getStylesSource().getCTStylesheet().getCellStyles());
                assertStylesAreValid(written);
            }
        }
    }

    /** Fails naming what a spreadsheet application would refuse in the style table of a workbook. */
    private static void assertStylesAreValid(XSSFWorkbook workbook) {
        var complaints = new ArrayList<XmlError>();
        var valid = workbook.getStylesSource().getCTStylesheet()
                .validate(new XmlOptions().setErrorListener(complaints));
        assertTrue(valid, () -> complaints.stream().map(String::valueOf).collect(Collectors.joining("; ")));
    }

    private static XSSFWorkbook reread(XSSFWorkbook workbook) throws IOException {
        var out = new ByteArrayOutputStream();
        workbook.write(out);
        return new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
    }
}
