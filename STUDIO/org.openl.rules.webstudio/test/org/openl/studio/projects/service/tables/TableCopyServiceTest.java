package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

    private final SystemPropertiesService systemPropertiesService = mock(SystemPropertiesService.class);
    private final TableCopyService service =
            new TableCopyServiceImpl(systemPropertiesService, new TableVersionService());
    private final TableCreatorService creator = new TableCreatorService(null, null, null, null, null);
    private final RawTableReader reader = new RawTableReader();

    @Test
    void copyKeepsTheBodyValuesStylesAndMerges(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        // Read the source with styles before writing the copy, so the copy is compared against the original.
        var sourceView = reader.read(source.table(), null, null, true);

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndexCopy", null, destGrid, tables(source));
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
                List.of(new TableProperty("state", "AL"), new TableProperty("lob", " ")), destGrid, tables(source));
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

        // The value reaches the table's right edge: left in its own column, the rest of the row would read as
        // cells of the properties section rather than as one value.
        var property = copyView.source.get(1);
        assertEquals("state", property.get(1).value());
        assertEquals(copyView.getWidth() - 2, property.get(2).colspan());
    }

    @Test
    void theCopyIsStampedAsCreated(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        when(systemPropertiesService.onCreate()).thenReturn(Map.of("createdBy", "jane"));

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndexCopy", List.of(new TableProperty("state", "AL")), destGrid,
                tables(source));
        creator.save(destGrid);

        // The copy is a new table, so it records its own author rather than inheriting the source's.
        var properties = resolve(projectDir, "BankLimitIndexCopy", "Copies").table().getProperties();
        assertEquals("jane", properties.getCreatedBy());
        assertEquals("AL", properties.getPropertyValueAsString("state"));
    }

    @Test
    void aDateTheSourceCarriesIsReadAndWrittenBackAsTheSameDate(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var sheet = sheetOf(source.table());
        writeProperty(source, "effectiveDate", "01/01/2009 12:00 AM");
        var dated = resolve(projectDir, "BankLimitIndex", sheet);
        var declaredDate = dated.table().getProperties().getEffectiveDate();

        // What the copy dialog is prefilled with: the date the source declares, in the form its picker reads.
        var prefilled = new TablePropertiesServiceImpl().read(dated.table());
        var effectiveDate = prefilled.stream().filter(property -> "effectiveDate".equals(property.name()))
                .findFirst().orElseThrow();
        assertEquals("2009-01-01", effectiveDate.value());

        // Sent back untouched, it is the same date again — and answers the same requests, so the copy is a new
        // version of the source rather than a table standing beside it.
        var destGrid = creator.sheetGridModel(dated.model(), "Copies");
        service.copyInto(dated.table(), "BankLimitIndex",
                List.of(effectiveDate, new TableProperty("version", "0.0.2")), destGrid, tables(dated));
        creator.save(destGrid);

        var copy = resolve(projectDir, "BankLimitIndex", "Copies").table().getProperties();
        assertEquals(declaredDate, copy.getEffectiveDate(), "the copy carries the date the source did");
        var replaced = resolve(projectDir, "BankLimitIndex", sheet).table().getProperties();
        assertEquals(Boolean.FALSE, replaced.getActive(), "the version it answers for steps aside");
    }

    @Test
    void aNewVersionTakesTheActiveRoleFromTheTableItReplaces(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var sourceSheet = sheetOf(source.table());

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndex", List.of(new TableProperty("version", "0.0.2")), destGrid,
                tables(source));
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
    void theRequestCannotWriteWhatOpenLStudioRecords(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        when(systemPropertiesService.onCreate()).thenReturn(Map.of("createdBy", "jane"));

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndexCopy", List.of(new TableProperty("createdBy", "someone else")),
                destGrid, tables(source));
        creator.save(destGrid);

        // The author a table reports is the one who made it, whatever the request says.
        assertEquals("jane", resolve(projectDir, "BankLimitIndexCopy", "Copies").table().getProperties().getCreatedBy());
    }

    @Test
    void aVersionAnotherVersionAlreadyCarriesIsRefused(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndex", List.of(new TableProperty("version", "0.0.2")), destGrid,
                tables(source));
        creator.save(destGrid);

        // The version the source stood for is carried by the table that stepped aside, so it is no longer free —
        // two versions under one number leave the engine unable to order them.
        var active = resolve(projectDir, "BankLimitIndex", "Copies");
        var table = active.table();
        var again = creator.sheetGridModel(active.model(), "Copies");
        var properties = List.of(new TableProperty("version", "0.0.1"));
        var moduleTables = tables(active);

        var refused = assertThrows(BadRequestException.class,
                () -> service.copyInto(table, "BankLimitIndex", properties, again, moduleTables));
        assertEquals("openl.error.400.table.copy.version.taken.message", refused.getErrorCode());
    }

    @Test
    void aCopyUnderAnotherNameLeavesTheSourceActive(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var sourceSheet = sheetOf(source.table());

        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        service.copyInto(source.table(), "BankLimitIndexCopy", List.of(new TableProperty("version", "0.0.2")),
                destGrid, tables(source));
        creator.save(destGrid);

        // A copy under another name is a table of its own, so nothing takes the source's place.
        var untouched = resolve(projectDir, "BankLimitIndex", sourceSheet).table().getProperties();
        assertNotEquals(Boolean.FALSE, untouched.getActive(), "a copy under another name leaves the source active");
    }

    @Test
    void aCopyAnsweringOtherRequestsLeavesTheSourceActive(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var dimensional = creator.sheetGridModel(source.model(), "Dimensional");
        service.copyInto(source.table(), "BankLimitIndex", List.of(new TableProperty("state", "AL")), dimensional,
                tables(source));
        creator.save(dimensional);

        var stateTable = resolve(projectDir, "BankLimitIndex", "Dimensional");
        var versions = creator.sheetGridModel(stateTable.model(), "Versions");
        service.copyInto(stateTable.table(), "BankLimitIndex", List.of(new TableProperty("version", "0.0.2")),
                versions, tables(stateTable));
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
                destGrid, tables(carried));
        creator.save(destGrid);

        assertEquals("1.0", resolve(projectDir, "BankLimitIndexCopy", "Legacy").table().getProperties().getVersion());
    }

    @Test
    void aCopyAskedToBeInactiveLeavesTheSourceActive(@TempDir Path projectDir) throws Exception {
        var source = bankLimitIndex(projectDir);
        var destGrid = creator.sheetGridModel(source.model(), "Copies");
        var sourceSheet = sheetOf(source.table());

        service.copyInto(source.table(), "BankLimitIndex",
                List.of(new TableProperty("version", "0.0.2"), new TableProperty("active", "No")), destGrid,
                tables(source));
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
                arguments("a copy that keeps the name and says nothing else", "BankLimitIndex", null),
                arguments("a copy that keeps the name and answers the same requests, with no version of its own",
                        "BankLimitIndex", List.of(new TableProperty("category", "Limits"))),
                arguments("a version number too long for the engine to order by", "BankLimitIndexCopy",
                        List.of(new TableProperty("version", "2147483648.0.0"))));
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

        var moduleTables = tables(source);

        assertThrows(BadRequestException.class,
                () -> service.copyInto(table, newName, properties, destGrid, moduleTables));
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
        var moduleTables = tables(retired);

        assertThrows(BadRequestException.class,
                () -> service.copyInto(table, "BankLimitIndex", properties, destGrid, moduleTables));
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

    /** The tables the module compiles, which a new version is checked against. */
    private static TableSyntaxNode[] tables(ResolvedTable resolved) {
        return resolved.model().getTableSyntaxNodes();
    }

    private record ResolvedTable(IOpenLTable table, ProjectModel model) {
    }
}
