package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;

/**
 * Builds the base, OUR and THEIR revisions of a workbook. OUR and THEIR revisions are edits of the base one. The
 * fixture merges them the way a merge of branches does.
 */
final class MergeFixture {

    static final String RULES = "Rules";
    private static final Consumer<Sheet> NO_EDIT = sheet -> {
    };
    private static final byte[] PNG = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    private final Consumer<Workbook> base;

    /**
     * @param base builds the base revision of the workbook, with a sheet named {@link #RULES}
     */
    MergeFixture(Consumer<Workbook> base) {
        this.base = base;
    }

    /**
     * Merges OUR and THEIR .xlsx revisions of the sheet against BASE revision, and returns the merged sheet.
     */
    Sheet mergeXlsx(Consumer<Sheet> our, Consumer<Sheet> their) throws IOException {
        return merge(XSSFWorkbook::new, our, their);
    }

    /**
     * Checks that the sheet stays a conflict, and is the only one.
     */
    void assertConflict(Consumer<Sheet> our, Consumer<Sheet> their) throws IOException {
        var diff = diff(XSSFWorkbook::new, our, their);
        assertTrue(diff.hasConflicts());
        assertEquals(List.of(RULES), List.copyOf(diff.getSheetDiffResult().getDiffSheets(DiffStatus.CONFLICT)));
    }

    /**
     * Merges OUR and THEIR revisions of the sheet against BASE revision, and returns the merged sheet.
     */
    Sheet merge(Supplier<Workbook> format, Consumer<Sheet> our, Consumer<Sheet> their) throws IOException {
        return merge(format, format, our, their);
    }

    /**
     * Merges OUR revision, written in its own format, and THEIR revision of the sheet against BASE revision.
     */
    Sheet merge(Supplier<Workbook> format, Supplier<Workbook> ourFormat, Consumer<Sheet> our, Consumer<Sheet> their)
            throws IOException {
        var ourBytes = write(ourFormat, our);
        var theirBytes = write(format, their);
        var diff = diff(write(format, NO_EDIT), ourBytes, theirBytes);
        assertFalse(diff.hasConflicts());
        var output = new ByteArrayOutputStream();
        XlsWorkbookMerger.merge(new ByteArrayInputStream(ourBytes), new ByteArrayInputStream(theirBytes), diff, output);
        return WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray())).getSheet(RULES);
    }

    WorkbookDiffResult diff(Supplier<Workbook> format, Consumer<Sheet> our, Consumer<Sheet> their) throws IOException {
        return diff(write(format, NO_EDIT), write(format, our), write(format, their));
    }

    private static WorkbookDiffResult diff(byte[] base, byte[] our, byte[] their) throws IOException {
        try (var merger = XlsWorkbookMerger.create(new ByteArrayInputStream(base),
                new ByteArrayInputStream(our),
                new ByteArrayInputStream(their))) {
            return merger.getDiffResult();
        }
    }

    private byte[] write(Supplier<Workbook> format, Consumer<Sheet> edit) throws IOException {
        try (var workbook = format.get(); var output = new ByteArrayOutputStream()) {
            base.accept(workbook);
            edit.accept(workbook.getSheet(RULES));
            workbook.write(output);
            return output.toByteArray();
        }
    }

    static void set(Sheet sheet, int row, int column, String value) {
        cellOf(sheet, row, column).setCellValue(value);
    }

    static void set(Sheet sheet, int row, int column, double value) {
        cellOf(sheet, row, column).setCellValue(value);
    }

    static Cell cellOf(Sheet sheet, int row, int column) {
        return CellUtil.getCell(CellUtil.getRow(row, sheet), column);
    }

    static Cell cell(Sheet sheet, int row, int column) {
        return SheetUtil.getCell(sheet, row, column);
    }

    /**
     * Adds a picture over an area of cells.
     */
    static void addPicture(Sheet sheet, String area) {
        var workbook = sheet.getWorkbook();
        var index = workbook.addPicture(PNG, Workbook.PICTURE_TYPE_PNG);
        var range = CellRangeAddress.valueOf(area);
        var anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setCol1(range.getFirstColumn());
        anchor.setRow1(range.getFirstRow());
        anchor.setCol2(range.getLastColumn());
        anchor.setRow2(range.getLastRow());
        sheet.createDrawingPatriarch().createPicture(anchor, index);
    }

    /**
     * Adds a comment to a cell, its box over an area of cells.
     */
    static void comment(Sheet sheet, int row, int column, String box, String text) {
        var workbook = sheet.getWorkbook();
        var area = CellRangeAddress.valueOf(box);
        var anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setRow1(area.getFirstRow());
        anchor.setRow2(area.getLastRow());
        anchor.setCol1(area.getFirstColumn());
        anchor.setCol2(area.getLastColumn());
        var comment = sheet.createDrawingPatriarch().createCellComment(anchor);
        comment.setString(workbook.getCreationHelper().createRichTextString(text));
        comment.setAuthor("John");
        cellOf(sheet, row, column).setCellComment(comment);
    }

    static double number(Sheet sheet, int row, int column) {
        return cell(sheet, row, column).getNumericCellValue();
    }

    static String text(Sheet sheet, int row, int column) {
        return cell(sheet, row, column).getStringCellValue();
    }
}
