package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableProperty;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Verifies that a copy is a faithful duplicate. The fixture holds {@code BankLimitIndex}, a formatted decision table
 * with a merged header and a {@code String[]} column, so the copy exercises styles, merges and a typed column at once.
 */
class TableCopyServiceTest {

    private static final Path FIXTURE = Path
            .of("test-resources/org/openl/studio/projects/service/tables/copy/BankLimits.xlsx");

    private final TableCopyService service = new TableCopyServiceImpl();
    private final TableCreatorService creator = new TableCreatorService(null, null, null, null);
    private final RawTableReader reader = new RawTableReader();

    @Test
    void copyKeepsTheBodyValuesStylesAndMerges(@TempDir Path projectDir) throws Exception {
        Files.copy(FIXTURE, projectDir.resolve("BankLimits.xlsx"));
        var source = resolve(projectDir, "BankLimitIndex");
        // Read the source with styles before writing the copy, so the copy is compared against the original.
        var sourceView = reader.read(source.table(), null, null, true);

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndexCopy", null, destGrid);
        creator.save(destGrid);

        var copyView = reader.read(resolve(projectDir, "BankLimitIndexCopy").table(), null, null, true);

        // The header is renamed after the copy but keeps the source header's style.
        assertTrue(String.valueOf(cell(copyView, 0, 0).value()).contains("BankLimitIndexCopy"),
                "the header is renamed after the copy");
        assertEquals(cell(sourceView, 0, 0).style(), cell(copyView, 0, 0).style(), "the header keeps its style");

        // Everything below the header — properties (kept) and body — is copied verbatim: values, styles and merges.
        assertEquals(sourceView.source.size(), copyView.source.size(), "the copy has the source's rows");
        for (int row = 1; row < sourceView.source.size(); row++) {
            var sourceRow = sourceView.source.get(row);
            var copyRow = copyView.source.get(row);
            assertEquals(sourceRow.size(), copyRow.size(), "row " + row + " has the source's width");
            for (int column = 0; column < sourceRow.size(); column++) {
                assertSameCell(sourceRow.get(column), copyRow.get(column), row, column);
            }
        }
    }

    @Test
    void copyWritesTheRequestedPropertiesInPlaceOfTheSource(@TempDir Path projectDir) throws Exception {
        Files.copy(FIXTURE, projectDir.resolve("BankLimits.xlsx"));
        var source = resolve(projectDir, "BankLimitIndex");

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndexCopy",
                List.of(new TableProperty("state", "AL"), new TableProperty("lob", " ")), destGrid);
        creator.save(destGrid);

        var copyView = reader.read(resolve(projectDir, "BankLimitIndexCopy").table(), null, null, true);
        var values = copyView.source.stream()
                .flatMap(List::stream)
                .map(RawTableCell::value)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
        assertTrue(values.contains("properties"), "a properties section is written");
        assertTrue(values.contains("state"), "the requested property name is written");
        // A blank value drops the property, so 'lob' never reaches the copy.
        assertTrue(values.stream().noneMatch("lob"::equals), "a blank-valued property is dropped");
    }

    /** Cells match on everything but their address: value, spans, covered flag and style. */
    private static void assertSameCell(RawTableCell expected, RawTableCell actual, int row, int column) {
        var at = " at " + row + "," + column;
        // deepEquals so a typed array column (String[]) is compared by content, not by reference.
        assertTrue(Objects.deepEquals(expected.value(), actual.value()),
                "value" + at + ": " + expected.value() + " vs " + actual.value());
        assertEquals(expected.colspan(), actual.colspan(), "colspan" + at);
        assertEquals(expected.rowspan(), actual.rowspan(), "rowspan" + at);
        assertEquals(expected.covered(), actual.covered(), "covered" + at);
        assertEquals(expected.style(), actual.style(), "style" + at);
    }

    private static RawTableCell cell(RawTableView view, int row, int column) {
        return view.source.get(row).get(column);
    }

    private static ResolvedTable resolve(Path projectDir, String name) throws Exception {
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        var projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            var header = String.valueOf(table.getGridTable().getCell(0, 0).getStringValue());
            if (header.contains(name)) {
                return new ResolvedTable(table, projectModel);
            }
        }
        throw new IllegalStateException(name + " not found in the fixture");
    }

    private record ResolvedTable(IOpenLTable table, ProjectModel model) {
    }
}
