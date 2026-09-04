package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;

/**
 * Verifies that a table's own properties are read in the form the copy dialog prefills from.
 */
class TablePropertiesServiceImplTest {

    private static final Path BANK_LIMITS = Path
            .of("test-resources/org/openl/studio/projects/service/tables/copy/BankLimits.xlsx");

    private final TablePropertiesService service = new TablePropertiesServiceImpl();

    @Test
    void readsNoPropertiesWhenTheTableDeclaresNone(@TempDir Path projectDir) throws Exception {
        Files.copy(BANK_LIMITS, projectDir.resolve("BankLimits.xlsx"));

        assertTrue(service.read(firstTable(projectDir)).isEmpty());
    }

    @Test
    void readsADeclaredProperty(@TempDir Path projectDir) throws Exception {
        writeDatatype(projectDir, "description", cell -> cell.setCellValue("copied table"));

        var properties = service.read(firstTable(projectDir));

        assertEquals(1, properties.size());
        assertEquals("description", properties.getFirst().name());
        assertEquals("copied table", properties.getFirst().value());
    }

    @Test
    void readsADateInIso(@TempDir Path projectDir) throws Exception {
        writeDatatype(projectDir, "createdOn", cell -> date(cell, LocalDate.of(2009, 1, 1)));

        var properties = service.read(firstTable(projectDir));

        // The dialog's date picker reads the value it is prefilled with, whatever the reader's locale.
        assertEquals(1, properties.size());
        assertEquals("createdOn", properties.getFirst().name());
        assertEquals("2009-01-01", properties.getFirst().value());
    }

    /** Writes a Datatype table whose properties section declares the named property, valued by the caller. */
    private static void writeDatatype(Path projectDir, String property, Consumer<Cell> value) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Types");
            row(sheet, 1, "Datatype Greeting");
            row(sheet, 2, "properties", property);
            value.accept(sheet.getRow(2).createCell(3));
            row(sheet, 3, "String", "name");
            try (OutputStream out = Files.newOutputStream(projectDir.resolve("Types.xlsx"))) {
                workbook.write(out);
            }
        }
    }

    /** Fills the cell with a date, in the date format a workbook stores one under. */
    private static void date(Cell cell, LocalDate value) {
        var workbook = cell.getSheet().getWorkbook();
        var dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("mm/dd/yyyy"));
        cell.setCellValue(value);
        cell.setCellStyle(dateStyle);
    }

    private static void row(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String... values) {
        var row = sheet.createRow(rowIndex);
        for (int column = 0; column < values.length; column++) {
            row.createCell(column + 1).setCellValue(values[column]);
        }
    }

    private static IOpenLTable firstTable(Path projectDir) throws Exception {
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        var projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            if (table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null && table.getProperties() != null) {
                return table;
            }
        }
        throw new IllegalStateException("No table with properties resolved in " + projectDir);
    }
}
