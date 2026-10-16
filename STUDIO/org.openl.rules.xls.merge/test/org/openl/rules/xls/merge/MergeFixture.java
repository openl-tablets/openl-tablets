package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol;

import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.UnresolvedConflictsException;
import org.openl.rules.xls.merge.diff.WorkbookConflicts;
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

    /**
     * Lists what OUR and THEIR revisions of the workbook changed in ways the merge cannot combine.
     */
    WorkbookConflicts conflicts(Supplier<Workbook> format, Consumer<Sheet> our, Consumer<Sheet> their)
            throws IOException {
        try (var merger = XlsWorkbookMerger.create(new ByteArrayInputStream(write(format, NO_EDIT)),
                new ByteArrayInputStream(write(format, our)),
                new ByteArrayInputStream(write(format, their)))) {
            return merger.getConflicts();
        }
    }

    /**
     * Merges OUR and THEIR .xlsx revisions with their conflicts resolved as decided, and returns the merged workbook.
     */
    Workbook resolveXlsx(Consumer<Sheet> our, Consumer<Sheet> their, Map<String, Resolution> decisions)
            throws IOException, UnresolvedConflictsException {
        return resolve(XSSFWorkbook::new, our, their, decisions);
    }

    /**
     * Merges OUR and THEIR revisions with their conflicts resolved as decided, and returns the merged workbook.
     */
    Workbook resolve(Supplier<Workbook> format,
                     Consumer<Sheet> our,
                     Consumer<Sheet> their,
                     Map<String, Resolution> decisions) throws IOException, UnresolvedConflictsException {
        var baseBytes = write(format, NO_EDIT);
        var ourBytes = write(format, our);
        var theirBytes = write(format, their);
        WorkbookDiffResult diff;
        try (var merger = XlsWorkbookMerger.create(new ByteArrayInputStream(baseBytes),
                new ByteArrayInputStream(ourBytes),
                new ByteArrayInputStream(theirBytes))) {
            diff = merger.resolve(decisions);
        }
        var output = new ByteArrayOutputStream();
        XlsWorkbookMerger.merge(new ByteArrayInputStream(baseBytes),
                new ByteArrayInputStream(ourBytes),
                new ByteArrayInputStream(theirBytes),
                diff,
                output);
        return WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()));
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

    /**
     * Lays out the columns of an .xlsx sheet from the first to the last one, counted from 1, as one layout.
     */
    static CTCol layOut(Sheet sheet, int first, int last) {
        var col = ((XSSFSheet) sheet).getCTWorksheet().getColsArray(0).addNewCol();
        col.setMin(first);
        col.setMax(last);
        return col;
    }

    /**
     * Writes an existing cell in bold.
     */
    static void bold(Sheet sheet, int row, int column) {
        var workbook = sheet.getWorkbook();
        var font = workbook.createFont();
        font.setBold(true);
        var style = workbook.createCellStyle();
        style.setFont(font);
        sheet.getRow(row).getCell(column).setCellStyle(style);
    }

    static boolean isBold(Sheet sheet, int row, int column) {
        var style = cell(sheet, row, column).getCellStyle();
        return sheet.getWorkbook().getFontAt(style.getFontIndex()).getBold();
    }

    static double number(Sheet sheet, int row, int column) {
        return cell(sheet, row, column).getNumericCellValue();
    }

    static String text(Sheet sheet, int row, int column) {
        return cell(sheet, row, column).getStringCellValue();
    }
    /**
     * A rules table at A1, a datatype table at E1 beside it and a data table at A9 below it, each with a merged
     * header.
     */
    static void tables(Workbook workbook) {
        var sheet = workbook.createSheet(RULES);
        set(sheet, 0, 0, "Rules String rate(String country)");
        sheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
        set(sheet, 1, 0, "C1");
        set(sheet, 1, 1, "RET1");
        set(sheet, 2, 0, "country");
        set(sheet, 2, 1, "rate");
        set(sheet, 3, 0, "US");
        set(sheet, 3, 1, 1);
        set(sheet, 4, 0, "CA");
        set(sheet, 4, 1, 2);
        set(sheet, 5, 0, "MX");
        set(sheet, 5, 1, 3);
        set(sheet, 0, 4, "Datatype Car");
        sheet.addMergedRegion(CellRangeAddress.valueOf("E1:F1"));
        set(sheet, 1, 4, "String");
        set(sheet, 1, 5, "model");
        set(sheet, 2, 4, "Integer");
        set(sheet, 2, 5, "year");
        set(sheet, 8, 0, "Data Driver drivers");
        sheet.addMergedRegion(CellRangeAddress.valueOf("A9:C9"));
        set(sheet, 9, 0, "name");
        set(sheet, 9, 1, "age");
        set(sheet, 9, 2, "note");
        set(sheet, 10, 0, "Ann");
        set(sheet, 10, 1, 30);
        set(sheet, 10, 2, "a");
        set(sheet, 11, 0, "Bob");
        set(sheet, 11, 1, 40);
        set(sheet, 11, 2, "b");
    }

    /**
     * Inserts a row into the rules table the way OpenL Studio does: the rows of the table below it move down into
     * the empty row under the table.
     */
    static void insertRow(Sheet sheet, int row, String country, double rate) {
        insertLine(sheet, row, 0);
        set(sheet, row, 0, country);
        set(sheet, row, 1, rate);
    }

    /**
     * Moves the rows of a two-column table from a row down by one, freeing that row.
     */
    static void insertLine(Sheet sheet, int row, int column) {
        var last = row;
        while (cell(sheet, last, column) != null) {
            last++;
        }
        if (last > row) {
            move(sheet, new CellRangeAddress(row, last - 1, column, column + 1).formatAsString(), 1, 0);
        }
    }

    /**
     * Deletes a row of the rules table the way OpenL Studio does: the rows of the table below it move up.
     */
    static void deleteRow(Sheet sheet, int row) {
        var sheetRow = sheet.getRow(row);
        sheetRow.removeCell(sheetRow.getCell(0));
        sheetRow.removeCell(sheetRow.getCell(1));
        var last = row + 1;
        while (cell(sheet, last, 0) != null) {
            last++;
        }
        if (last > row + 1) {
            move(sheet, "A" + (row + 2) + ":B" + last, -1, 0);
        }
    }

    /**
     * Moves a table with its merged header by rows and columns, the way OpenL Studio moves a table.
     */
    static void moveTable(Sheet sheet, String table, String header, int rows, int columns) {
        sheet.removeMergedRegion(regionIndex(sheet, header));
        move(sheet, table, rows, columns);
        var region = CellRangeAddress.valueOf(header);
        sheet.addMergedRegion(new CellRangeAddress(region.getFirstRow() + rows,
                region.getLastRow() + rows,
                region.getFirstColumn() + columns,
                region.getLastColumn() + columns));
    }

    /**
     * Removes a table with its merged header.
     */
    static void removeTable(Sheet sheet, String table, String header) {
        sheet.removeMergedRegion(regionIndex(sheet, header));
        for (var address : CellRangeAddress.valueOf(table)) {
            var cell = cell(sheet, address.getRow(), address.getColumn());
            if (cell != null) {
                cell.getRow().removeCell(cell);
            }
        }
    }

    /**
     * Moves the cells of a block, their values and styles, by rows and columns.
     */
    static void move(Sheet sheet, String block, int rows, int columns) {
        record Moved(int row, int column, CellType type, String text, double number, CellStyle style) {
        }
        var moved = new ArrayList<Moved>();
        for (var address : CellRangeAddress.valueOf(block)) {
            var cell = cell(sheet, address.getRow(), address.getColumn());
            if (cell != null) {
                var type = cell.getCellType();
                moved.add(new Moved(address.getRow() + rows,
                        address.getColumn() + columns,
                        type,
                        type == CellType.STRING ? cell.getStringCellValue() : "",
                        type == CellType.NUMERIC ? cell.getNumericCellValue() : 0,
                        cell.getCellStyle()));
                cell.getRow().removeCell(cell);
            }
        }
        for (Moved cell : moved) {
            var target = cellOf(sheet, cell.row(), cell.column());
            switch (cell.type()) {
                case STRING -> target.setCellValue(cell.text());
                case NUMERIC -> target.setCellValue(cell.number());
                default -> target.setBlank();
            }
            target.setCellStyle(cell.style());
        }
    }

    static int regionIndex(Sheet sheet, String region) {
        return sheet.getMergedRegions().indexOf(CellRangeAddress.valueOf(region));
    }
}
