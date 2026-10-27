package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.UndoableSetValueAction;
import org.openl.rules.table.xls.writers.XlsCellDateWriter;
import org.openl.rules.table.xls.writers.XlsCellNumberWriter;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.types.java.JavaOpenClass;

class XlsCellNumberWriterTest {

    public interface SpecialNumberRules {

        Double special(int input);
    }

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
    void savesAndCompilesNonFiniteNumbers(String text) throws IOException {
        var path = createRuleWorkbook();
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        when(table.getGrid()).thenReturn(grid);
        when(metaInfoWriter.getMetaInfo(5, 2))
                .thenReturn(new CellMetaInfo(JavaOpenClass.getOpenClass(Double.class), false));
        doAnswer(invocation -> {
            writeValue(path, invocation.getArgument(2, Number.class));
            return null;
        }).when(grid).setCellValue(eq(2), eq(5), any());

        new UndoableSetValueAction(2, 5, text, metaInfoWriter).doAction(table);

        try (var workbook = WorkbookFactory.create(path.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(5).getCell(2);
            assertEquals(CellType.STRING, cell.getCellType());
            assertEquals(text, cell.getStringCellValue());
        }

        var rules = new RulesEngineFactory<>(path.toString(), SpecialNumberRules.class).newEngineInstance();
        assertEquals(Double.valueOf(text), rules.special(1));
    }

    @Test
    void keepsFiniteNumbersNumeric() throws IOException {
        var path = createRuleWorkbook();
        writeValue(path, 1.5);

        try (var workbook = WorkbookFactory.create(path.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(5).getCell(2);
            assertEquals(CellType.NUMERIC, cell.getCellType());
            assertEquals(1.5, cell.getNumericCellValue());
        }
    }

    /** A number is stored as Excel stores it: a whole one without a fraction, whatever type it was written as. */
    @ParameterizedTest
    @CsvSource({"67, 67", "67.0, 67", "-3.0, -3", "-0.0, 0", "1.5, 1.5", "1.0E20, 1.0E20"})
    void storesNumbersAsExcel(String written, String stored) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var writer = new XlsCellNumberWriter(mock(XlsSheetGridModel.class));
            writer.setCellToWrite(cell);
            // The cast keeps the conditional from unboxing an Integer into a double.
            writer.setValueToWrite(written.contains(".") ? (Number) Double.valueOf(written) : Integer.valueOf(written));

            writer.writeCellValue();

            assertEquals(stored, cell.getCTCell().getV());
            assertFalse(cell.getCTCell().isSetT(), "a number is the type a cell has by default");
            // Excel has no negative zero, so -0.0 reads back as 0.
            assertEquals(Double.parseDouble(written), cell.getNumericCellValue(), 0.0);
        }
    }

    /** A date of no time of the day is stored as the whole number Excel counts days by. */
    @Test
    void storesWholeDaysAsExcel() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var grid = mock(XlsSheetGridModel.class, RETURNS_DEEP_STUBS);
            when(grid.getSheetSource().getSheet().getWorkbook()).thenReturn(workbook);
            var writer = new XlsCellDateWriter(grid);
            writer.setCellToWrite(cell);
            writer.setValueToWrite(new GregorianCalendar(2023, Calendar.MARCH, 15).getTime());

            writer.writeCellValue();

            assertEquals("45000", cell.getCTCell().getV());
        }
    }

    @Test
    void savesFloatNaNAsText() throws IOException {
        var path = createRuleWorkbook();
        writeValue(path, Float.NaN);

        try (var workbook = WorkbookFactory.create(path.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(5).getCell(2);
            assertEquals(CellType.STRING, cell.getCellType());
            assertEquals("NaN", cell.getStringCellValue());
        }
    }

    @Test
    void replacesInlineStringAndCompilesNewValue() throws IOException {
        var path = createRuleWorkbook();
        var inlinePath = tempDir.resolve("inline-special-numbers.xlsx");
        try (var workbook = (XSSFWorkbook) WorkbookFactory.create(path.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(5).getCell(2);
            cell.setBlank();
            cell.getCTCell().setT(STCellType.INLINE_STR);
            cell.getCTCell().addNewIs().setT("NaN");
            try (var output = Files.newOutputStream(inlinePath)) {
                workbook.write(output);
            }
        }
        Files.move(inlinePath, path, StandardCopyOption.REPLACE_EXISTING);

        var source = new XlsWorkbookSourceCodeModule(new PathSourceCodeModule(path));
        new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, source)).setCellValue(2, 5, Double.POSITIVE_INFINITY);
        source.save();

        try (var workbook = WorkbookFactory.create(path.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(5).getCell(2);
            assertEquals("Infinity", cell.getStringCellValue());
        }
        var rules = new RulesEngineFactory<>(path.toString(), SpecialNumberRules.class).newEngineInstance();
        assertEquals(Double.POSITIVE_INFINITY, rules.special(1));
    }

    @Test
    void preservesFormulaWhenWritingNonFiniteValue() throws IOException {
        var path = tempDir.resolve("formula.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            cell.setCellFormula("1+1");
            var writer = new XlsCellNumberWriter(mock(XlsSheetGridModel.class));
            writer.setCellToWrite(cell);
            writer.setValueToWrite(Double.NaN);
            writer.writeCellValue();
            try (var output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }

        try (var workbook = WorkbookFactory.create(path.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(0).getCell(0);
            assertEquals(CellType.FORMULA, cell.getCellType());
            assertEquals("1+1", cell.getCellFormula());
            assertEquals("NaN", cell.getStringCellValue());
        }
    }

    private Path createRuleWorkbook() throws IOException {
        var path = tempDir.resolve("special-numbers.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Rules");
            sheet.createRow(3).createCell(1).setCellValue("SimpleRules Double special(int input)");
            var header = sheet.createRow(4);
            header.createCell(1).setCellValue("input");
            header.createCell(2).setCellValue("value");
            var data = sheet.createRow(5);
            data.createCell(1).setCellValue(1);
            data.createCell(2).setCellValue(1);
            try (var output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
        return path;
    }

    private void writeValue(Path path, Number value) throws IOException {
        var updatedPath = tempDir.resolve("updated-special-numbers.xlsx");
        try (var workbook = WorkbookFactory.create(path.toFile())) {
            var writer = new XlsCellNumberWriter(mock(XlsSheetGridModel.class));
            writer.setCellToWrite(workbook.getSheetAt(0).getRow(5).getCell(2));
            writer.setValueToWrite(value);
            writer.writeCellValue();
            try (var output = Files.newOutputStream(updatedPath)) {
                workbook.write(output);
            }
        }
        Files.move(updatedPath, path, StandardCopyOption.REPLACE_EXISTING);
    }
}
