package org.openl.rules.table.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriterImpl;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.PathSourceCodeModule;

/**
 * Inserts columns into a table and compiles the saved workbook.
 *
 * @author Yury Molchan
 */
class UndoableInsertColumnsActionTest {

    private static final int DATA_TOP = 7;
    private static final int LEFT = 1;

    @TempDir
    Path folder;

    /**
     * A workbook written by another tool, such as openpyxl, keeps its texts in the cells. Shifting a number onto a
     * cell that held such a text leaves the number alone in the cell.
     */
    @Test
    void columnInsertedBeforeNumberKeepsInlineStringWorkbookCompiling() throws IOException {
        var path = inlineStringWorkbook();
        var workbook = new XlsWorkbookSourceCodeModule(new PathSourceCodeModule(path));
        var grid = new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, workbook));
        var table = Arrays.stream(grid.getTables())
                .filter(found -> found.getRegion().getTop() == DATA_TOP)
                .findFirst()
                .orElseThrow();
        var metaInfoWriter = new MetaInfoWriterImpl(EmptyMetaInfoReader.getInstance(), table);

        new UndoableInsertColumnsAction(1, 1, metaInfoWriter).doAction(table);
        grid.setCellValue(LEFT + 1, DATA_TOP + 1, "zip");
        grid.setCellValue(LEFT + 1, DATA_TOP + 2, "Zip");
        grid.setCellValue(LEFT + 1, DATA_TOP + 3, "02101");
        workbook.save();

        var rules = new RulesEngineFactory<>(path.toString(), PersonRules.class).newEngineInstance();
        assertEquals("John", rules.nameOf(0));
        assertEquals("02101", rules.zipOf(0));
        assertEquals("Boston", rules.cityOf(0));
        assertEquals(30, rules.ageOf(0));
        assertEquals("CA", rules.stateOf(0));
    }

    /** Lays out a datatype, a data table of it and methods reading the data, every text kept in its cell. */
    private Path inlineStringWorkbook() throws IOException {
        var path = folder.resolve("inline-strings.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Rules");
            var row = 0;
            texts(sheet.createRow(row++), "Datatype Person");
            texts(sheet.createRow(row++), "String", "name");
            texts(sheet.createRow(row++), "String", "zip");
            texts(sheet.createRow(row++), "String", "city");
            texts(sheet.createRow(row++), "Integer", "age");
            texts(sheet.createRow(row), "String", "state");

            row = DATA_TOP;
            texts(sheet.createRow(row++), "Data Person persons");
            texts(sheet.createRow(row++), "name", "city", "age", "state");
            texts(sheet.createRow(row++), "Name", "City", "Age", "State");
            var data = sheet.createRow(row++);
            texts(data, "John", "Boston");
            data.createCell(LEFT + 2).setCellValue(30);
            text(data, LEFT + 3, "CA");

            row++;
            for (var field : new String[] { "name", "zip", "city", "state" }) {
                row = method(sheet.createRow(row), sheet.createRow(row + 1), "String", field);
            }
            method(sheet.createRow(row), sheet.createRow(row + 1), "Integer", "age");

            try (var output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
        return path;
    }

    private static int method(XSSFRow header, XSSFRow body, String type, String field) {
        texts(header, "Method " + type + " " + field + "Of(int index)");
        texts(body, "return persons[index]." + field + ";");
        return body.getRowNum() + 2;
    }

    private static void texts(XSSFRow row, String... texts) {
        for (var i = 0; i < texts.length; i++) {
            text(row, LEFT + i, texts[i]);
        }
    }

    /** Writes a text into the cell the way openpyxl does: inline, not in the shared strings. */
    private static void text(XSSFRow row, int column, String text) {
        var cell = row.createCell(column).getCTCell();
        cell.setT(STCellType.INLINE_STR);
        cell.addNewIs().setT(text);
    }

    public interface PersonRules {
        String nameOf(int index);

        String zipOf(int index);

        String cityOf(int index);

        Integer ageOf(int index);

        String stateOf(int index);
    }
}
