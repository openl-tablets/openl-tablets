package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.PropertyInheritance;
import org.openl.studio.projects.model.tables.TableDetailsView;
import org.openl.studio.projects.model.tables.TablePropertyDetailView;
import org.openl.studio.projects.model.tables.TablePropertyGroupView;

/**
 * Verifies that a table is described by every property that applies to it, inherited ones included.
 *
 * <p>The module of the fixture declares three properties for all of its tables, and the rules table declares two
 * of its own — one of which the module also declares, so the table's own value wins.
 */
class TableDetailsServiceImplTest {

    private final TableDetailsService service = new TableDetailsServiceImpl();

    private ProjectModel projectModel;

    @BeforeEach
    void writeModule(@TempDir Path projectDir) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Rules");
            row(sheet, 1, "Properties");
            row(sheet, 2, "scope", "Module");
            row(sheet, 3, "lob", "Banking");
            row(sheet, 4, "buildPhase", "main");
            row(sheet, 5, "effectiveDate");
            date(sheet.getRow(5).createCell(2), LocalDate.of(2009, 1, 1));

            row(sheet, 7, "Rules String Hello()");
            // The properties section of a table spans as many rows as it declares properties.
            row(sheet, 8, "properties", "description", "Says hello");
            row(sheet, 9, null, "lob", "Insurance");
            sheet.addMergedRegion(new CellRangeAddress(8, 9, 1, 1));
            row(sheet, 10, "", "Hello");

            try (OutputStream out = Files.newOutputStream(projectDir.resolve("Rules.xlsx"))) {
                workbook.write(out);
            }
        }
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
    }

    @Test
    void describesATableByItsOwnPropertiesAndTheOnesItInherits() {
        var details = service.read(table(XlsNodeTypes.XLS_DT));

        assertEquals("Hello", details.name());
        // The dictionary decides both the groups and the order inside them.
        assertEquals(List.of("Info", "Business Dimension", "Dev"),
                details.groups().stream().map(TablePropertyGroupView::name).toList());
        assertEquals(List.of("description"), names(details.groups().getFirst().properties()));
        assertEquals(List.of("effectiveDate", "lob"), names(details.groups().get(1).properties()));
        assertEquals(List.of("buildPhase"), names(details.groups().getLast().properties()));
    }

    @Test
    void marksAPropertyWrittenOnTheTableAsItsOwn() {
        var description = property(service.read(table(XlsNodeTypes.XLS_DT)), "description");

        assertEquals("Description", description.displayName());
        assertEquals("Says hello", description.value());
        assertNull(description.inheritedFrom());
        assertNull(description.inheritedTableId());
    }

    @Test
    void namesThePropertiesTableAnInheritedValueComesFrom() {
        var effectiveDate = property(service.read(table(XlsNodeTypes.XLS_DT)), "effectiveDate");

        assertEquals("2009-01-01", effectiveDate.value());
        assertEquals(PropertyInheritance.MODULE, effectiveDate.inheritedFrom());
        assertEquals(table(XlsNodeTypes.XLS_PROPERTIES).getId(), effectiveDate.inheritedTableId());
    }

    @Test
    void letsTheTableOverrideWhatItsModuleDeclares() {
        var lob = property(service.read(table(XlsNodeTypes.XLS_DT)), "lob");

        assertEquals("Insurance", lob.value());
        assertNull(lob.inheritedFrom());
    }

    @Test
    void describesNothingForATableThatCarriesNoProperties() {
        assertTrue(service.read(table(XlsNodeTypes.XLS_PROPERTIES)).groups().isEmpty());
    }

    private static List<String> names(List<TablePropertyDetailView> properties) {
        return properties.stream().map(TablePropertyDetailView::name).toList();
    }

    private static TablePropertyDetailView property(TableDetailsView details, String name) {
        var property = details.groups()
                .stream()
                .flatMap(group -> group.properties().stream())
                .filter(candidate -> name.equals(candidate.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(property, "No '" + name + "' property in " + details);
        return property;
    }

    /** The single table of the module of the given kind. */
    private IOpenLTable table(XlsNodeTypes type) {
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            if (type.toString().equals(tsn.getType())) {
                return new TableSyntaxNodeAdapter(tsn);
            }
        }
        throw new IllegalStateException("No " + type + " table in the module");
    }

    /** Fills the cell with a date, in the date format a workbook stores one under. */
    private static void date(Cell cell, LocalDate value) {
        var workbook = cell.getSheet().getWorkbook();
        var dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("mm/dd/yyyy"));
        cell.setCellValue(value);
        cell.setCellStyle(dateStyle);
    }

    private static void row(Sheet sheet, int rowIndex, String... values) {
        var row = sheet.createRow(rowIndex);
        for (int column = 0; column < values.length; column++) {
            if (values[column] != null) {
                row.createCell(column + 1).setCellValue(values[column]);
            }
        }
    }
}
