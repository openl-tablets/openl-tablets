package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.SummaryTableView;

/**
 * Verifies that a table is read with what tells it from the other versions of itself, and with the mark that says
 * it takes no part in the rules.
 *
 * <p>The module of the fixture writes one table in two versions, told apart by the line of business each works for,
 * and one table of its own that is switched off.
 */
class SummaryTableReaderTest {

    private final SummaryTableReader reader = new SummaryTableReader();

    private ProjectModel projectModel;

    @BeforeEach
    void writeModule(@TempDir Path projectDir) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Rules");
            carPrice(sheet, 1, "Banking", "Cheap");
            carPrice(sheet, 9, "Insurance", "Dear");

            row(sheet, 17, "Rules String Discount()");
            row(sheet, 18, "properties", "active", "false");
            row(sheet, 19, "", "None");

            try (OutputStream out = Files.newOutputStream(projectDir.resolve("Rules.xlsx"))) {
                workbook.write(out);
            }
        }
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
    }

    @Test
    void namesEveryVersionOfATableByWhatTellsThemApart() {
        var versions = tables("CarPrice");

        assertEquals(List.of("CarPrice [lob=Banking]", "CarPrice [lob=Insurance]"),
                versions.stream().map(table -> table.displayName).sorted().toList());
        // The versions of one table are gathered under the signature they share.
        assertEquals(List.of("CarPrice(java.lang.String)"),
                versions.stream().map(table -> table.overloadGroup).distinct().toList());
    }

    @Test
    void leavesATableThatHasNoOtherVersionUnnamed() {
        var discount = tables("Discount").getFirst();

        assertNull(discount.displayName);
        assertNull(discount.overloadGroup);
        assertEquals("Discount", discount.name);
    }

    @Test
    void saysNothingAboutVersionsWhenItIsNotAskedWithTheDictionary() {
        var read = projectModel.getAllTableSyntaxNodes()
                .stream()
                .map(TableSyntaxNodeAdapter::new)
                .map(reader::read)
                .filter(table -> "CarPrice".equals(table.name))
                .toList();

        assertEquals(List.of(), read.stream().map(table -> table.displayName).filter(name -> name != null).toList());
    }

    @Test
    void marksTheTableThatIsSwitchedOff() {
        assertEquals(Boolean.FALSE, tables("Discount").getFirst().active);
        // Nothing is said about a table that takes part in the rules.
        tables("CarPrice").forEach(table -> assertNull(table.active));
    }

    /** The tables of the module carrying the given name, read with what the module knows about their versions. */
    private List<SummaryTableView> tables(String name) {
        var overloads = projectModel.getMethodNodesDictionary();
        return projectModel.getAllTableSyntaxNodes()
                .stream()
                .map(TableSyntaxNodeAdapter::new)
                .map(table -> reader.read(table, overloads))
                .filter(table -> name.equals(table.name))
                .toList();
    }

    /** Writes one version of the CarPrice table, the one working for the given line of business. */
    private static void carPrice(Sheet sheet, int firstRow, String lob, String price) {
        row(sheet, firstRow, "Rules String CarPrice(String make)");
        row(sheet, firstRow + 1, "properties", "lob", lob);
        row(sheet, firstRow + 2, "C1", "RET1");
        row(sheet, firstRow + 3, "make == c1", "value");
        row(sheet, firstRow + 4, "String c1", "String value");
        row(sheet, firstRow + 5, "Make", "Price");
        row(sheet, firstRow + 6, "Toyota", price);
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
