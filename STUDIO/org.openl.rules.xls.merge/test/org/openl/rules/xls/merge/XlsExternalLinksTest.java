package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.openl.rules.xls.merge.MergeFixture.set;

import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.model.ExternalLinksTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsExternalLinksTest {

    private static final MergeFixture FIXTURE = new MergeFixture(MergeFixture::tables);

    @Test
    void sheetOfTheirsReadingAnotherWorkbook_bringsTheLink() throws IOException {
        var merged = (XSSFWorkbook) FIXTURE.mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            link(sheet, "Census.xlsx");
            var cell = sheet.getWorkbook().createSheet("Linked").createRow(0).createCell(0);
            cell.setCellFormula("[1]Census!A1");
            cell.setCellValue(7);
        }).getWorkbook();

        assertEquals(List.of("Census.xlsx"), linkedFiles(merged));
        var cell = merged.getSheet("Linked").getRow(0).getCell(0);
        assertEquals("[1]Census!A1", cell.getCellFormula());
        // the other workbook is not at hand, so the formula keeps the value it was saved with
        assertEquals(7, cell.getNumericCellValue());
        assertEquals(10, MergeFixture.number(merged.getSheet(MergeFixture.RULES), 3, 1));
    }

    @Test
    void formulaOfTheirsReadingAWorkbookOursLinksElsewhere_isNotMerged() {
        var e = assertThrows(LinkedWorkbookMismatchException.class, () -> FIXTURE.mergeXlsx(sheet -> {
            set(sheet, 3, 1, 10);
            link(sheet, "Rates.xlsx");
        }, sheet -> {
            link(sheet, "Census.xlsx");
            sheet.getWorkbook().createSheet("Linked").createRow(0).createCell(0).setCellFormula("[1]Census!A1");
        }));

        assertEquals("Linked", e.getSheet());
        assertEquals("A1", e.getCell());
        assertEquals(1, e.getLink());
    }

    @Test
    void workbooksLinkingDifferentWorkbooksUnderOneNumber_mergeWhatReadsNeither() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> {
            set(sheet, 3, 1, 10);
            link(sheet, "Rates.xlsx");
        }, sheet -> {
            set(sheet, 4, 1, 20);
            link(sheet, "Census.xlsx");
        });

        assertEquals(List.of("Rates.xlsx"), linkedFiles((XSSFWorkbook) merged.getWorkbook()));
        assertEquals(10, MergeFixture.number(merged, 3, 1));
        assertEquals(20, MergeFixture.number(merged, 4, 1));
    }

    @Test
    void linkNoFormulaOfTheirsReads_isNotBrought() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 4, 1, 20);
            link(sheet, "Census.xlsx");
        });

        assertEquals(List.of(), linkedFiles((XSSFWorkbook) merged.getWorkbook()));
    }

    @Test
    void formulaCopied_readsTheWorkbookItsOwnWorkbookLinksUnderTheNumber() throws IOException {
        try (var source = new XSSFWorkbook();
             var same = new XSSFWorkbook();
             var other = new XSSFWorkbook();
             var none = new XSSFWorkbook()) {
            var formula = linkedSheet(source, "Census.xlsx").createRow(0).createCell(0);
            formula.setCellFormula("[1]Census!A1");

            XlsExternalLinks.linkWorkbooks(formula, cellIn(linkedSheet(same, "Census.xlsx")));
            var otherCell = cellIn(linkedSheet(other, "Rates.xlsx"));
            assertThrows(LinkedWorkbookMismatchException.class,
                    () -> XlsExternalLinks.linkWorkbooks(formula, otherCell));
            // a workbook that lacks the link gets it under the same number
            XlsExternalLinks.linkWorkbooks(formula, cellIn(none.createSheet("Linked")));
            assertEquals(List.of("Census.xlsx"), linkedFiles(none));
        }
    }

    @Test
    void textThatLooksLikeALink_isNoLink() throws IOException {
        try (var source = new XSSFWorkbook(); var other = new XSSFWorkbook()) {
            var text = linkedSheet(source, "Census.xlsx").createRow(0).createCell(0);
            text.setCellFormula("\"[1]\"&B1");

            XlsExternalLinks.linkWorkbooks(text, cellIn(linkedSheet(other, "Rates.xlsx")));

            assertEquals(List.of("Rates.xlsx"), linkedFiles(other));
        }
    }

    /** A DDE or an OLE link names no workbook: it is not brought, and nothing compares it. */
    @Test
    void linkNamingNoWorkbook_isNotBrought() throws IOException {
        try (var their = new XSSFWorkbook(); var our = new XSSFWorkbook()) {
            var formula = linkedSheet(their, "Census.xlsx").createRow(0).createCell(0);
            formula.setCellFormula("[1]Census!A1");
            their.getExternalLinksTables().getFirst().getCTExternalLink().unsetExternalBook();

            XlsExternalLinks.linkWorkbooks(formula, cellIn(our.createSheet("Linked")));

            assertEquals(List.of(), linkedFiles(our));
        }
    }

    private static List<String> linkedFiles(XSSFWorkbook workbook) {
        return workbook.getExternalLinksTables().stream().map(ExternalLinksTable::getLinkedFileName).toList();
    }

    /** A sheet of a workbook that links another workbook under the number 1. */
    private static Sheet linkedSheet(XSSFWorkbook workbook, String workbookName) {
        var sheet = workbook.createSheet("Linked");
        link(sheet, workbookName);
        return sheet;
    }

    private static Cell cellIn(Sheet sheet) {
        return sheet.createRow(0).createCell(0);
    }

    /**
     * Links the workbook of a sheet to another workbook, which is not kept.
     */
    private static void link(Sheet sheet, String workbookName) {
        try (var other = new XSSFWorkbook()) {
            ((XSSFWorkbook) sheet.getWorkbook()).linkExternalWorkbook(workbookName, other);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
