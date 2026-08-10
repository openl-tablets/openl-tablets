package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.DatatypeFieldView;
import org.openl.studio.projects.model.tables.DatatypeView;
import org.openl.studio.projects.service.tables.read.DatatypeTableReader;
import org.openl.studio.projects.service.tables.read.RawTableReader;
import org.openl.studio.projects.service.tables.write.DatatypeTableWriter;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;

/**
 * A datatype table has six columns, and only a table that names them can carry more than the first three. These
 * tests write a workbook, read it back through {@link DatatypeTableReader}, write it again through
 * {@link DatatypeTableWriter}, and reload from disk — the in-memory grid is stale after a save.
 */
class DatatypeTableIOTest {

    @TempDir
    Path tempDir;

    @Test
    void readsEveryColumnATitledBodyNames() throws IOException {
        var project = writeProject("titled", new String[][]{
                {"Datatype Claim", null, null, null, null, null},
                {"Type", "Name", "Default", "Mandatory", "Description", "Example"},
                {"String", "city", "Kyiv", "true", "Where it happened", "Lviv"}
        });

        var field = onlyFieldOf(project);
        assertEquals("city", field.name);
        assertEquals("String", field.type);
        assertEquals("Kyiv", field.defaultValue);
        assertEquals("true", field.mandatory);
        assertEquals("Where it happened", field.description);
        assertEquals("Lviv", field.example);
    }

    @Test
    void readsALegacyBodyByPosition() throws IOException {
        // No title row, so the first row is already a field and only the three positional columns exist.
        var project = writeProject("legacy", new String[][]{
                {"Datatype Claim", null, null},
                {"String", "city", "Kyiv"}
        });

        var field = onlyFieldOf(project);
        assertEquals("city", field.name);
        assertEquals("Kyiv", field.defaultValue);
        assertNull(field.mandatory);
        assertNull(field.description);
        assertNull(field.example);
    }

    @Test
    void readsATitledBodyInItsOwnOrder() throws IOException {
        // The third column is a description, not a default: a titled body may order its columns as it likes.
        var project = writeProject("reordered", new String[][]{
                {"Datatype Claim", null, null},
                {"Name", "Type", "Description"},
                {"city", "String", "Where it happened"}
        });

        var field = onlyFieldOf(project);
        assertEquals("city", field.name);
        assertEquals("String", field.type);
        assertEquals("Where it happened", field.description);
        assertNull(field.defaultValue, "a body that names no Default column carries none");
    }

    @Test
    void writesBackIntoTheColumnsTheTitlesName() throws IOException {
        var project = writeProject("write-titled", new String[][]{
                {"Datatype Claim", null, null, null},
                {"Name", "Description", "Type", "Mandatory"},
                {"city", "Where it happened", "String", "true"}
        });

        var view = read(load(project));
        write(project, view, List.of(DatatypeFieldView.builder()
                .name("town")
                .type("Integer")
                .description("Renamed")
                .mandatory("false")
                .build()));

        var source = rawSourceOf(project);
        assertEquals(List.of("Name", "Description", "Type", "Mandatory"), source.get(1),
                "the titles must survive the write");
        assertEquals(List.of("town", "Renamed", "Integer", "false"), source.get(2),
                "each value must land under its own title");
    }

    @Test
    void rejectsAValueTheTableKeepsNoColumnFor() throws IOException {
        var project = writeProject("no-column", new String[][]{
                {"Datatype Claim", null, null},
                {"String", "city", "Kyiv"}
        });
        var view = read(load(project));
        var withDescription = List.of(DatatypeFieldView.builder()
                .name("city")
                .type("String")
                .description("nowhere to put this")
                .build());

        // Silently dropping the description would look like a successful save that lost the value.
        assertThrows(BadRequestException.class, () -> write(project, view, withDescription));
    }

    @Test
    void createsATitleRowWhenAFieldNeedsMoreThanTheThreeColumns() throws IOException {
        var project = writeProject("create-titled", new String[][]{{"Environment"}});

        create(project, "create-titled", "Claim", List.of(DatatypeFieldView.builder()
                .name("city")
                .type("String")
                .defaultValue("Kyiv")
                .description("Where it happened")
                .build()));

        var source = rawSourceOf(project);
        assertEquals(List.of("Type", "Name", "Default", "Description"), source.get(1),
                "the columns a positional layout cannot tell apart must be named");
        assertEquals(List.of("String", "city", "Kyiv", "Where it happened"), source.get(2));
        // A datatype the writer created has to read back as the same datatype.
        var field = onlyFieldOf(project);
        assertEquals("Where it happened", field.description);
        assertEquals("Kyiv", field.defaultValue);
    }

    @Test
    void namesOnlyTheColumnsTheFieldsActuallyUse() throws IOException {
        // No field carries a description or a default, so neither column is written at all.
        var project = writeProject("used-columns", new String[][]{{"Environment"}});

        create(project, "used-columns", "Claim", List.of(DatatypeFieldView.builder()
                .name("city")
                .type("String")
                .mandatory("true")
                .example("Lviv")
                .build()));

        var source = rawSourceOf(project);
        assertEquals(List.of("Type", "Name", "Mandatory", "Example"), source.get(1));
        assertEquals(List.of("String", "city", "true", "Lviv"), source.get(2));
    }

    @Test
    void rejectsADefaultForABodyTooNarrowToHoldOne() throws IOException {
        // A two-column legacy body has no Default column, so writing one would widen the table and shift
        // whatever sits beside it on the sheet.
        var project = writeProject("narrow", new String[][]{
                {"Datatype Claim", null},
                {"String", "city"}
        });
        var view = read(load(project));
        var withDefault = List.of(DatatypeFieldView.builder().name("city").type("String").defaultValue("Kyiv").build());

        assertThrows(BadRequestException.class, () -> write(project, view, withDefault));
    }

    @Test
    void treatsABlankValueAsNoValue() throws IOException {
        // A form that submits its empty inputs must not reshape the table around columns that stay empty.
        var project = writeProject("blank-values", new String[][]{{"Environment"}});

        create(project, "blank-values", "Claim", List.of(DatatypeFieldView.builder()
                .name("city")
                .type("String")
                .defaultValue("Kyiv")
                .description("")
                .example("")
                .build()));

        var source = rawSourceOf(project);
        assertEquals(2, source.size(), "blank columns must not add a title row");
        assertEquals(List.of("String", "city", "Kyiv"), source.get(1));
    }

    @Test
    void keepsThePositionalLayoutWhenAFieldCarriesNoMoreThanADefault() throws IOException {
        var project = writeProject("create-legacy", new String[][]{{"Environment"}});

        create(project, "create-legacy", "Claim", List.of(DatatypeFieldView.builder()
                .name("city")
                .type("String")
                .defaultValue("Kyiv")
                .build()));

        var source = rawSourceOf(project);
        assertEquals(2, source.size(), "a header and one field, with no title row between them");
        assertEquals(List.of("String", "city", "Kyiv"), source.get(1));
    }

    private static DatatypeFieldView onlyFieldOf(Path project) {
        var fields = read(load(project)).fields;
        assertEquals(1, fields.size(), "the fixture declares one field");
        return fields.iterator().next();
    }

    private static DatatypeView read(IOpenLTable table) {
        return new DatatypeTableReader().read(table);
    }

    /** Rewrite the datatype of {@code project} with the given fields, keeping its name and properties. */
    private static void write(Path project, DatatypeView view, List<DatatypeFieldView> fields) {
        new DatatypeTableWriter(load(project)).write(DatatypeView.builder()
                .name(view.name)
                .kind(view.kind)
                .fields(fields)
                .build());
    }

    /** Create a datatype beside the tables the project already has, the way the create endpoint does. */
    private static void create(Path project, String sheetName, String name, List<DatatypeFieldView> fields) {
        var view = DatatypeView.builder().name(name).fields(fields).build();
        var writer = new TableWritersFactory().getNewTableWriter(view, sheetGrid(project, sheetName));
        ((DatatypeTableWriter) writer).write(view);
    }

    /**
     * A writable grid over the sheet the project keeps its tables on. The resolved project only reads its
     * workbook, so the create endpoint's own grid is used here rather than the one the tables were read from.
     */
    private static XlsSheetGridModel sheetGrid(Path project, String sheetName) {
        var service = new TableCreatorService(null, null, null, null);
        return service.sheetGridModel(projectModel(project), sheetName);
    }

    /** The datatype's cells as plain text, row by row, as they sit on the reloaded sheet. */
    private static List<List<String>> rawSourceOf(Path project) {
        var rows = new ArrayList<List<String>>();
        for (var row : new RawTableReader().read(load(project)).source) {
            rows.add(row.stream().map(cell -> cell.value() == null ? null : String.valueOf(cell.value())).toList());
        }
        return rows;
    }

    /** The one datatype of the single-module project. Fixtures declare no more than one. */
    private static IOpenLTable load(Path dir) {
        for (TableSyntaxNode tsn : projectModel(dir).getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            if (table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null && OpenLTableUtils.isDatatypeTable(table)) {
                return table;
            }
        }
        throw new IllegalStateException("No datatype resolved in " + dir);
    }

    /** Resolve and compile the single-module project at {@code dir}. */
    private static ProjectModel projectModel(Path dir) {
        try {
            var modules = ProjectResolver.getInstance().resolve(dir).getModules();
            var projectModel = new ProjectModel(mock(WebStudio.class), null);
            projectModel.setModuleInfo(modules.getFirst());
            return projectModel;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve project at " + dir, e);
        }
    }

    /** Write a single-sheet workbook holding one table that starts at cell B2. {@code null} cells stay blank. */
    private Path writeProject(String name, String[][] grid) throws IOException {
        var dir = tempDir.resolve(name);
        Files.createDirectories(dir);
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(name);
            for (var r = 0; r < grid.length; r++) {
                var sheetRow = sheet.createRow(r + 1);
                for (var c = 0; c < grid[r].length; c++) {
                    if (grid[r][c] != null) {
                        sheetRow.createCell(c + 1).setCellValue(grid[r][c]);
                    }
                }
            }
            try (OutputStream out = Files.newOutputStream(dir.resolve(name + ".xlsx"))) {
                workbook.write(out);
            }
        }
        return dir;
    }
}
