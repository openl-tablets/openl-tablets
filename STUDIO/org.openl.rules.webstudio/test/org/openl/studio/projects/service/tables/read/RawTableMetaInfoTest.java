package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableUsageKind;
import org.openl.studio.projects.service.tables.TableModules;

/**
 * Verifies that a table is read with what the compiler knows about its cells.
 *
 * <p>The module of the fixture declares two datatypes, one holding a field of the other. The compiler resolves
 * the name of that other datatype inside the field's cell, which is what the Editor draws as a link: a range
 * over the cell's text, the table it leads to, and what the text stands for.
 */
class RawTableMetaInfoTest {

    private ProjectModel projectModel;

    @BeforeEach
    void writeModule(@TempDir Path projectDir) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Types");
            row(sheet, 1, "Datatype Address");
            row(sheet, 2, "String", "city");

            row(sheet, 4, "Datatype Person");
            row(sheet, 5, "String", "name");
            row(sheet, 6, "Address", "address");

            try (OutputStream out = Files.newOutputStream(projectDir.resolve("Types.xlsx"))) {
                workbook.write(out);
            }
        }
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
    }

    @Test
    void marksWhatACellRefersToAndWhereItLeads() {
        var person = table("Person");

        var read = new RawTableReader().read(person, null, null, false, true,
                TableModules.ofWorkspace(projectModel));

        var typeCell = cellOf(read.source, "Address");
        var metaInfo = typeCell.metaInfo();
        assertNotNull(metaInfo, "the cell names a datatype, so the compiler knows something about it");
        assertEquals(1, metaInfo.usages().size());
        var usage = metaInfo.usages().getFirst();
        // The range covers the name itself, so a screen marks exactly the characters that refer to something.
        assertEquals("Address", String.valueOf(typeCell.value()).substring(usage.start(), usage.end()));
        assertEquals(RawTableUsageKind.DATATYPE, usage.kind());
        // The table is named by the identifier the Tables API addresses it by, not by where the engine keeps it.
        assertEquals(TableUtils.makeTableId(table("Address").getUri()), usage.tableId());
        // The editor opens a module and reads a table through it, so the usage names that module too.
        assertEquals(projectModel.getModuleInfo().getName(), usage.module());
        assertNotNull(usage.description());
    }

    @Test
    void saysNothingAboutCellsUntilItIsAsked() {
        var read = new RawTableReader().read(table("Person"), null, null, false, false, TableModules.none());

        assertTrue(read.source.stream().flatMap(List::stream).allMatch(cell -> cell.metaInfo() == null),
                "the compiler's knowledge is read only when the read asks for it");
    }

    @Test
    void keepsQuietAboutACellItKnowsNothingAbout() {
        var read = new RawTableReader().read(table("Person"), null, null, false, true, TableModules.none());

        // The header line is the table's own; nothing in it refers anywhere.
        assertNull(cellOf(read.source, "Datatype Person").metaInfo());
    }

    /** The cell whose value reads exactly like the given text. */
    private static RawTableCell cellOf(List<List<RawTableCell>> source, String value) {
        return source.stream()
                .flatMap(List::stream)
                .filter(cell -> value.equals(cell.value()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No cell reading '" + value + "'"));
    }

    /** The table of the module whose name ends with the given one. */
    private IOpenLTable table(String name) {
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            if (name.equals(table.getName())) {
                return table;
            }
        }
        throw new IllegalStateException("No table named " + name);
    }

    private static void row(Sheet sheet, int rowIndex, String... values) {
        var row = sheet.createRow(rowIndex);
        for (int column = 0; column < values.length; column++) {
            row.createCell(column + 1).setCellValue(values[column]);
        }
    }
}
