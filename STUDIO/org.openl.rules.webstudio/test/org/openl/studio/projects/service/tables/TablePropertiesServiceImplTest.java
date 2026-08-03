package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
 * Verifies that a table's own properties are read in the display form the copy dialog prefills from.
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
        writeDatatypeWithDescription(projectDir);

        var properties = service.read(firstTable(projectDir));

        assertEquals(1, properties.size());
        assertEquals("description", properties.getFirst().name());
        assertEquals("copied table", properties.getFirst().value());
    }

    /** Writes a Datatype table whose properties section declares a single {@code description} property. */
    private static void writeDatatypeWithDescription(Path projectDir) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Types");
            row(sheet, 1, "Datatype Greeting");
            row(sheet, 2, "properties", "description", "copied table");
            row(sheet, 3, "String", "name");
            try (OutputStream out = Files.newOutputStream(projectDir.resolve("Types.xlsx"))) {
                workbook.write(out);
            }
        }
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
