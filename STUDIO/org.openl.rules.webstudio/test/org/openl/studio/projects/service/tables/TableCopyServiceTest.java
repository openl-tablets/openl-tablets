package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.BadRequestException;
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
        var source = bankLimitIndex(projectDir);
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
        var source = bankLimitIndex(projectDir);

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

    @Test
    void aNewVersionTakesTheActiveRoleFromTheTableItReplaces(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var sourceSheet = sheetOf(source.table());

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndex", List.of(new TableProperty("version", "0.0.2")), destGrid);
        creator.save(destGrid);

        // Two active tables of one name do not compile, so the table the new version replaces steps aside.
        var replaced = resolve(projectDir, "BankLimitIndex", sourceSheet).table().getProperties();
        assertEquals(Boolean.FALSE, replaced.getActive(), "the replaced table is no longer active");
        assertEquals("0.0.1", replaced.getVersion(), "the replaced table keeps the version it stood for");

        var copy = resolve(projectDir, "BankLimitIndex", "Copies").table().getProperties();
        assertEquals("0.0.2", copy.getVersion(), "the copy is the new version");
        assertNotEquals(Boolean.FALSE, copy.getActive(), "the copy is the active version");
    }

    @Test
    void aCopyUnderAnotherNameLeavesTheSourceActive(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var sourceSheet = sheetOf(source.table());

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndexCopy", List.of(new TableProperty("version", "0.0.2")),
                destGrid);
        creator.save(destGrid);

        // A copy under another name is a table of its own, so nothing takes the source's place.
        var untouched = resolve(projectDir, "BankLimitIndex", sourceSheet).table().getProperties();
        assertNotEquals(Boolean.FALSE, untouched.getActive(), "a copy under another name leaves the source active");
    }

    @Test
    void aCopyAnsweringOtherRequestsLeavesTheSourceActive(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var dimensional = creator.sheetGridModel(source.model(), "Dimensional");
        service.copyInto(source.table(), "BankLimitIndex", List.of(new TableProperty("state", "AL")), dimensional);
        creator.save(dimensional);

        var stateTable = resolve(projectDir, "BankLimitIndex", "Dimensional");
        var versions = creator.sheetGridModel(stateTable.model(), "Versions");
        service.copyInto(stateTable.table(), "BankLimitIndex", List.of(new TableProperty("version", "0.0.2")),
                versions);
        creator.save(versions);

        // The copy declares no state, so the engine dispatches it for other requests: it stands beside the table
        // it was copied from rather than replacing it, and that table must keep answering for 'AL'.
        var untouched = resolve(projectDir, "BankLimitIndex", "Dimensional").table().getProperties();
        assertNotEquals(Boolean.FALSE, untouched.getActive(), "a copy of another dispatch group replaces nothing");
    }

    @Test
    void theVersionTheSourceCarriesIsCopiedAsItStands(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var sheet = sheetOf(source.table());
        // A version written when a shorter form was documented as valid: the table must stay copyable.
        writeProperty(source, "version", "1.0");
        var carried = resolve(projectDir, "BankLimitIndex", sheet);

        var destGrid = creator.sheetGridModel(carried.model(), "Legacy");
        service.copyInto(carried.table(), "BankLimitIndexCopy", List.of(new TableProperty("version", "1.0")),
                destGrid);
        creator.save(destGrid);

        assertEquals("1.0", resolve(projectDir, "BankLimitIndexCopy", "Legacy").table().getProperties().getVersion());
    }

    @Test
    void aCopyAskedToBeInactiveLeavesTheSourceActive(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        var sourceSheet = sheetOf(source.table());

        service.copyInto(source.table(), "BankLimitIndex",
                List.of(new TableProperty("version", "0.0.2"), new TableProperty("active", "No")), destGrid);
        creator.save(destGrid);

        // 'No' is one of the ways the engine reads false, so the copy is staged rather than taking over, and the
        // table that answers today must keep answering.
        var untouched = resolve(projectDir, "BankLimitIndex", sourceSheet).table().getProperties();
        assertNotEquals(Boolean.FALSE, untouched.getActive(), "a copy staged as inactive replaces nothing");
    }

    static Stream<Arguments> refusedCopies() {
        return Stream.of(
                arguments("a version the engine cannot read", "BankLimitIndex",
                        List.of(new TableProperty("version", "v2"))),
                arguments("a version that repeats the one it replaces", "BankLimitIndex",
                        List.of(new TableProperty("version", "0.0.1"))),
                arguments("the same version padded with spaces", "BankLimitIndex",
                        List.of(new TableProperty("version", " 0.0.1 "))),
                arguments("a version the source does not carry, under another name", "BankLimitIndexCopy",
                        List.of(new TableProperty("version", "1.0"))),
                arguments("a copy that keeps the name and says nothing else", "BankLimitIndex", null));
    }

    /**
     * A copy that would leave two tables the engine cannot tell apart is refused.
     *
     * <p>Versions are ordered by their numbers, so a version of any other shape reads as the same version as every
     * other unreadable one. A version that repeats the one it replaces, and a copy that keeps the name while saying
     * nothing about itself, end the same way — the source declares no version, so it stands for the initial one.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("refusedCopies")
    void aCopyLeavingTwoTablesIndistinguishableIsRefused(String refused, String newName,
            @Nullable List<TableProperty> properties, @TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var table = source.table();
        var destGrid = creator.sheetGridModel(source.model(), "Copies");

        assertThrows(BadRequestException.class, () -> service.copyInto(table, newName, properties, destGrid));
    }

    @Test
    void aNewVersionMadeFromARetiredTableIsRefused(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var sheet = sheetOf(source.table());
        writeProperty(source, "active", "false");

        var retired = resolve(projectDir, "BankLimitIndex", sheet);
        var table = retired.table();
        var destGrid = creator.sheetGridModel(retired.model(), "Copies");
        var properties = List.of(new TableProperty("version", "0.0.2"));

        // The table that answers today is another one, so which version this copy would replace cannot be told.
        assertThrows(BadRequestException.class, () -> service.copyInto(table, "BankLimitIndex", properties, destGrid));
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

    /** Writes a property onto the table itself, the way a table already in the repository carries one. */
    private void writeProperty(ResolvedTable table, String name, String value) {
        var grid = table.table().getGridTable();
        grid.edit();
        try {
            new TableEditorModel(table.table(), IXlsTableNames.VIEW_DEVELOPER, false).setProperty(name, value);
        } finally {
            grid.stopEditing();
        }
        creator.save(creator.sheetGridModel(table.model(), sheetOf(table.table())));
    }

    private static String sheetOf(IOpenLTable table) {
        return table.getSyntaxNode().getXlsSheetSourceCodeModule().getSheetName();
    }

    /** The fixture's {@code BankLimitIndex}, in a project directory of its own. */
    private static ResolvedTable bankLimitIndex(Path projectDir) throws Exception {
        Files.copy(FIXTURE, projectDir.resolve("BankLimits.xlsx"));
        return resolve(projectDir, "BankLimitIndex");
    }

    private static ResolvedTable resolve(Path projectDir, String name) throws Exception {
        return resolve(projectDir, name, null);
    }

    /** The table whose header names it, on that sheet when one is named: a copy may keep the source's name. */
    private static ResolvedTable resolve(Path projectDir, String name, @Nullable String sheetName) throws Exception {
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        var projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            var header = String.valueOf(table.getGridTable().getCell(0, 0).getStringValue());
            if (header.contains(name) && (sheetName == null || sheetName.equals(sheetOf(table)))) {
                return new ResolvedTable(table, projectModel);
            }
        }
        throw new IllegalStateException(name + " not found in the fixture");
    }

    private record ResolvedTable(IOpenLTable table, ProjectModel model) {
    }
}
