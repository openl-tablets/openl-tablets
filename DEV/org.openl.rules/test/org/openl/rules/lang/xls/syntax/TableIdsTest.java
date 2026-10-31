package org.openl.rules.lang.xls.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.excel.grid.SequentialXlsLoader;
import org.openl.excel.parser.markdown.MarkdownGrid;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.IProjectSourceCodeModule;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.types.IMemberMetaInfo;
import org.openl.util.HashingUtils;

class TableIdsTest {

    private static final String RULES = "rules/Main.xlsx";

    @TempDir
    private Path dir;

    @Test
    void anIdIsSixteenUpperCaseCharactersOfBase32() throws IOException {
        var project = project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1"));

        var id = only(parse(project, RULES)).getId();

        assertTrue(id.matches("[0-9A-HJKMNP-TV-Z]{16}"), id);
    }

    @Test
    void aTableKeepsItsIdWhereverItStandsInItsWorkbookAndWhereverTheProjectIs() throws IOException {
        var first = workbook(dir.resolve("a").resolve(RULES), workbook -> {
            var sheet = workbook.createSheet("Rules");
            put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1");
            put(sheet, 5, 1, "Datatype Car", "String|model");
        });
        // Another user's workspace: rows inserted above, the rule cut to the end of the sheet, the datatype
        // pasted onto another sheet.
        workbook(dir.resolve("b").resolve(RULES), workbook -> {
            var sheet = workbook.createSheet("Rules");
            put(sheet, 4, 2, "Datatype Note", "String|text");
            put(sheet, 30, 1, "Rules String rate(String state)", "C1|RET1", "\"CA\"|1");
            put(workbook.createSheet("Types"), 0, 0, "Datatype Car", "String|model");
        });

        var a = ids(parse(first.getParent().getParent(), RULES));
        var b = ids(parse(dir.resolve("b"), RULES));

        assertEquals(a.get("Rules String rate(String state)"), b.get("Rules String rate(String state)"));
        assertEquals(a.get("Datatype Car"), b.get("Datatype Car"));
    }

    /** The version of a table changes with the text of a cell, and only with it. */
    @Test
    void aTableHasANewVersionWhenTheTextOfACellChanges() throws IOException {
        var a = project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1", "\"CA\"|1"));
        var moved = project("b", sheet -> put(sheet, 7, 3, "Rules String rate(String state)", "C1|RET1", "\"CA\"|1"));
        var edited = project("c", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1", "\"CA\"|2"));

        var table = only(parse(a, RULES));
        var version = table.getVersion();

        assertEquals(version, table.getVersion(), "the version stays while the table is not compiled again");
        assertEquals(version, only(parse(moved, RULES)).getVersion(), "where the table stands is not its content");
        assertNotEquals(version, only(parse(edited, RULES)).getVersion());
        assertEquals(version, new TableSyntaxNodeAdapter(table).getVersion());
    }

    /** The version is the digest of the text of the cells, row by row: the versions told before stay valid. */
    @Test
    void theVersionIsTheDigestOfTheTextOfTheCells() throws IOException {
        var table = only(parse(project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1",
                "\"CA\"|1")), RULES));

        assertEquals(HashingUtils.sha256Hex("Rules String rate(String state)\tnull\t\nC1\tRET1\t\n\"CA\"\t1\t\n"),
                table.getVersion());
    }

    /** A written table takes a new version at once, while the module is not compiled again, and keeps it. */
    @Test
    void aWrittenTableTakesANewVersion() throws IOException {
        var table = only(parse(project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1",
                "\"CA\"|1")), RULES));
        var read = table.getVersion();

        table.markWritten();

        var written = table.getVersion();
        assertNotEquals(read, written);
        assertEquals(written, table.getVersion());
        new TableSyntaxNodeAdapter(table).markWritten();
        assertNotEquals(written, table.getVersion(), "the table read by OpenL Studio is the table itself");
    }

    @Test
    void aRenameGivesANewIdAndSoDoesAnotherModule() throws IOException {
        var project = project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1"));
        var rate = only(parse(project, RULES)).getId();
        project("b", sheet -> put(sheet, 1, 1, "Rules String price(String state)", "C1|RET1"));
        workbook(dir.resolve("a").resolve("rules/Other.xlsx"),
                workbook -> put(workbook.createSheet("Rules"), 1, 1, "Rules String rate(String state)", "C1|RET1"));

        assertNotEquals(rate, only(parse(dir.resolve("b"), RULES)).getId());
        assertNotEquals(rate, only(parse(project, "rules/Other.xlsx")).getId());
    }

    @Test
    void parametersReturnTypeAndPropertiesAreNotPartOfTheId() throws IOException {
        var project = project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "properties|state|CA",
                "C1|RET1"));
        project("b", sheet -> put(sheet, 1, 1, "SmartRules Double rate (String state, Integer age)",
                "properties|state|NY", "properties|lob|Auto", "C1|RET1"));

        assertEquals(only(parse(project, RULES)).getId(), only(parse(dir.resolve("b"), RULES)).getId());
    }

    @Test
    void theIdPropertyNamesARuleTable() throws IOException {
        var plain = project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1"));
        var named = project("b", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "properties|id|rateCA",
                "C1|RET1"));
        var renamed = project("c", sheet -> put(sheet, 1, 1, "Rules String price(Integer age)",
                "properties|id|rateCA", "C1|RET1"));

        var id = only(parse(named, RULES)).getId();

        assertNotEquals(only(parse(plain, RULES)).getId(), id);
        assertEquals(id, only(parse(renamed, RULES)).getId());
    }

    @Test
    void everyKindIsToldByItsKey() throws IOException {
        var project = project("a", sheet -> {
            put(sheet, 1, 1, "Datatype Car extends Vehicle", "String|model");
            put(sheet, 5, 1, "Datatype Gender <String>", "male");
            put(sheet, 9, 1, "Data Car cars", "model");
            put(sheet, 13, 1, "Test rate rateTest", "state|_res_");
            put(sheet, 17, 1, "Run rate", "state");
            put(sheet, 21, 1, "Constants", "Integer|MAX|10");
            put(sheet, 25, 1, "Environment", "import|java.util");
            put(sheet, 29, 1, "Properties", "scope|Module");
            put(sheet, 33, 1, "Some   free   text", "note");
            put(sheet, 37, 1, "Rules", "C1|RET1");
            put(sheet, 41, 1, "Spreadsheet SpreadsheetResult calc", "Step|Value");
            put(sheet, 45, 1, "Conditions", "C1");
        });
        var changed = project("b", sheet -> {
            put(sheet, 1, 1, "Datatype Car extends Thing", "String|model");
            put(sheet, 5, 1, "Datatype Gender <Integer>", "1");
            put(sheet, 9, 1, "Data Vehicle cars", "model");
            put(sheet, 13, 1, "Test price rateTest", "state|_res_");
            put(sheet, 17, 1, "Run rate", "age");
            put(sheet, 21, 1, "Constants", "Integer|MIN|1");
            put(sheet, 25, 1, "Environment", "import|java.time");
            put(sheet, 29, 1, "Properties", "scope|Module", "lob|Auto");
            put(sheet, 33, 1, "Some free text", "other");
            put(sheet, 37, 1, "Rules", "C2|RET2");
            put(sheet, 41, 1, "Spreadsheet Double calc()", "Step|Value");
            put(sheet, 45, 1, "Conditions", "C2");
        });

        var before = parse(project, RULES).getXlsTableSyntaxNodes();
        var after = parse(changed, RULES).getXlsTableSyntaxNodes();

        var distinct = new HashSet<String>();
        for (var i = 0; i < before.length; i++) {
            assertEquals(before[i].getId(), after[i].getId(), before[i].getGridTable().getCell(0, 0).getStringValue());
            distinct.add(before[i].getId());
        }
        assertEquals(before.length, distinct.size(), "every table has an id of its own");
    }

    @Test
    void tablesSharingAnIdentityAreNumberedInTheOrderTheModuleHoldsThem() throws IOException {
        var project = project("a", sheet -> {
            put(sheet, 1, 1, "Test calc", "first|_res_");
            put(sheet, 5, 1, "Rules String rate(String state)", "properties|state|CA", "C1|RET1");
            put(sheet, 10, 1, "Test calc", "second|_res_");
            put(sheet, 14, 1, "Rules String rate(String state)", "properties|state|NY", "C1|RET1");
        });
        var swapped = project("b", sheet -> {
            put(sheet, 1, 1, "Test calc", "second|_res_");
            put(sheet, 10, 1, "Test calc", "first|_res_");
        });

        var tests = byFirstBodyCell(parse(project, RULES));
        var tables = parse(project, RULES).getXlsTableSyntaxNodes();
        var reordered = byFirstBodyCell(parse(swapped, RULES));

        assertNotEquals(tests.get("first"), tests.get("second"));
        assertNotEquals(tables[1].getId(), tables[3].getId(), "two versions of one rule in one workbook");
        // Moved past each other outside OpenL Studio, they exchange their numbers.
        assertEquals(tests.get("first"), reordered.get("second"));
        assertEquals(tests.get("second"), reordered.get("first"));
    }

    @Test
    void aTableStudioMovesKeepsItsNumber() throws IOException {
        var project = project("a", sheet -> {
            put(sheet, 1, 1, "Test calc", "first|_res_");
            put(sheet, 5, 1, "Test calc", "second|_res_");
        });
        var file = project.resolve(RULES);
        var module = parse(project, RULES);
        var first = module.getXlsTableSyntaxNodes()[0];
        var ids = byFirstBodyCell(module);
        // The write had no room to grow the first test, so it moved it below the second one.
        workbook(file, workbook -> {
            var sheet = workbook.createSheet("Rules");
            put(sheet, 5, 1, "Test calc", "second|_res_");
            put(sheet, 9, 1, "Test calc", "first|_res_", "added|1");
        });

        var written = new GridTable(9, 1, 11, 2, grid(project, file));

        assertEquals(ids.get("first"), TableIds.idAfterWrite(first, written));
        assertEquals(ids, byFirstBodyCell(parse(project, RULES)));
    }

    @Test
    void theTablesAfterADeletedOneKeepTheirIds() throws IOException {
        var project = project("a", sheet -> {
            put(sheet, 1, 1, "Test calc", "first|_res_");
            put(sheet, 5, 1, "Test calc", "second|_res_");
            put(sheet, 9, 1, "Test calc", "third|_res_");
        });
        var module = parse(project, RULES);
        var ids = byFirstBodyCell(module);

        TableIds.deleting(module.getXlsTableSyntaxNodes()[0]);
        workbook(project.resolve(RULES), workbook -> {
            var sheet = workbook.createSheet("Rules");
            put(sheet, 5, 1, "Test calc", "second|_res_");
            put(sheet, 9, 1, "Test calc", "third|_res_");
        });

        var after = byFirstBodyCell(parse(project, RULES));
        assertEquals(ids.get("second"), after.get("second"));
        assertEquals(ids.get("third"), after.get("third"));
        assertFalse(after.containsValue(ids.get("first")), "no table takes the id of the deleted one");
    }

    @Test
    void aMovedWorkbookGivesItsTablesTheIdsTheyHaveAtItsNewPlace() throws IOException {
        var project = project("a", sheet -> {
            put(sheet, 1, 1, "Test calc", "first|_res_");
            put(sheet, 5, 1, "Test calc", "second|_res_");
            put(sheet, 9, 1, "Rules String rate(String state)", "C1|RET1");
        });
        var file = project.resolve(RULES);
        var first = parse(project, RULES).getXlsTableSyntaxNodes()[0];
        // OpenL Studio moved the first test below the others, where it keeps its number.
        workbook(file, workbook -> {
            var sheet = workbook.createSheet("Rules");
            put(sheet, 5, 1, "Test calc", "second|_res_");
            put(sheet, 9, 1, "Rules String rate(String state)", "C1|RET1");
            put(sheet, 13, 1, "Test calc", "first|_res_", "added|1");
        });
        TableIds.idAfterWrite(first, new GridTable(13, 1, 15, 2, grid(project, file)));
        var module = parse(project, RULES);
        var ids = byFirstBodyCell(module);

        var moved = TableIds.idsAfterMove(module, project.resolve("rules"), project.resolve("lib/rates"));
        Files.createDirectories(project.resolve("lib"));
        Files.move(project.resolve("rules"), project.resolve("lib/rates"));

        var after = byFirstBodyCell(parse(project, "lib/rates/Main.xlsx"));
        assertEquals(3, moved.size());
        ids.forEach((cell, id) -> assertEquals(after.get(cell), moved.get(id), cell));
        assertTrue(TableIds.idsAfterMove(module, project.resolve("other"), project.resolve("lib")).isEmpty(),
                "a move of another file renames no table");
    }

    @Test
    void aTableOfAMarkdownModuleIsNamedAfterTheModuleFolderNotAfterItsFile() throws IOException {
        markdown(dir.resolve("a/rules/Main/greeting.md"), "SmartRules String greeting(Integer hour)");
        // Another user's workspace: the file renamed and moved to a sub-folder of the module
        markdown(dir.resolve("b/rules/Main/Hello/salutation.md"), "SmartRules String greeting(Integer hour)");
        markdown(dir.resolve("c/rules/Other/greeting.md"), "SmartRules String greeting(Integer hour)");

        var id = only(parse(dir.resolve("a"), "rules/Main")).getId();

        assertEquals(id, only(parse(dir.resolve("b"), "rules/Main")).getId());
        assertNotEquals(id, only(parse(dir.resolve("c"), "rules/Other")).getId(), "another module, another id");
    }

    /** The version a write names back in If-Match is the digest of the cells of the table, not of its file. */
    @Test
    void theVersionOfATableOfAMarkdownFileChangesWithItsCellsAlone() throws IOException {
        var project = dir.resolve("a");
        var file = project.resolve("rules/Main/greeting.md");
        markdown(file, "SmartRules String greeting(Integer hour)");
        var version = only(parse(project, "rules/Main")).getVersion();

        assertEquals(version, only(parse(project, "rules/Main")).getVersion(), "the same file compiled again");
        // Text around the table and the table aligned by hand are no cells of it
        var text = Files.readString(file);
        Files.writeString(file, text.replace("|---|---|", "|------|----------|") + "\nBy the hour.\n");
        assertEquals(version, only(parse(project, "rules/Main")).getVersion());
        Files.writeString(file, Files.readString(file).replace("Good Morning", "Morning"));
        assertNotEquals(version, only(parse(project, "rules/Main")).getVersion());
    }

    @Test
    void aFileMovedWithinItsModuleFolderKeepsItsIdAndAMovedFolderGivesANewOne() throws IOException {
        var project = dir.resolve("a");
        var folder = project.resolve("rules/Main");
        markdown(folder.resolve("greeting.md"), "SmartRules String greeting(Integer hour)");
        var module = parse(project, "rules/Main");
        var id = only(module).getId();

        assertTrue(TableIds.idsAfterMove(module, folder.resolve("greeting.md"), folder.resolve("Hello/hi.md"))
                .isEmpty(), "a file moved within the folder keeps the id of its table");
        var moved = TableIds.idsAfterMove(module, folder, project.resolve("lib/Main"));
        Files.createDirectories(project.resolve("lib"));
        Files.move(folder, project.resolve("lib/Main"));

        assertEquals(Map.of(id, only(parse(project, "lib/Main")).getId()), moved);
    }

    @Test
    void aTableWrittenIntoAMarkdownFileIsKnownByTheIdItsModuleGivesIt() throws IOException {
        var project = dir.resolve("a");
        markdown(project.resolve("rules/Main/greeting.md"), "SmartRules String greeting(Integer hour)");
        var table = only(parse(project, "rules/Main"));
        var grid = MarkdownGrid.open(table.getXlsSheetSourceCodeModule());
        var region = table.getGridTable().getRegion();
        var written = new GridTable(region.getTop(), region.getLeft(), region.getBottom(), region.getRight(), grid);

        assertEquals(table.getId(), TableIds.idAfterWrite(table, written), "a table still what it was keeps its id");
        grid.setCellValue(0, 0, "SmartRules String salutation(Integer hour)");
        var renamed = TableIds.idAfterWrite(table, written);
        grid.save();

        assertNotEquals(table.getId(), renamed);
        assertEquals(only(parse(project, "rules/Main")).getId(), renamed);
    }

    @Test
    void theIdsOfAModuleAreToldByTheCellsTheirTablesStartAt() throws IOException {
        var module = parse(project("a", sheet -> {
            put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1");
            put(sheet, 5, 2, "Datatype Car", "String|model");
        }), RULES);

        assertFalse(TableIds.isNumbered(module));
        var ids = TableIds.idsByCell(module);

        assertTrue(TableIds.isNumbered(module));
        var tables = module.getXlsTableSyntaxNodes();
        var sheet = dir.resolve("a").resolve(RULES).toAbsolutePath().normalize() + "!Rules!";
        assertEquals(Map.of(sheet + "1!1", tables[0].getId(), sheet + "5!2", tables[1].getId()), ids);
    }

    @Test
    void aWrittenTableIsKnownByTheIdItsModuleGivesIt() throws IOException {
        var project = project("a", sheet -> {
            put(sheet, 1, 1, "Test calc", "first|_res_");
            put(sheet, 5, 1, "Rules String rate(String state)", "C1|RET1");
        });
        var file = project.resolve(RULES);
        var module = parse(project, RULES);
        workbook(file, workbook -> {
            var sheet = workbook.createSheet("Rules");
            put(sheet, 1, 1, "Test calc", "first|_res_");
            put(sheet, 5, 1, "Rules String rate(String state)", "C1|RET1");
            put(sheet, 9, 1, "Test calc", "second|_res_");
            put(workbook.createSheet("Types"), 1, 1, "Datatype Car", "String|model");
        });

        var test = TableIds.idOfWritten(new GridTable(9, 1, 10, 2, grid(project, file)), module);
        var datatype = TableIds.idOfWritten(new GridTable(1, 1, 2, 2, grid(project, file, 1)), module);

        var compiled = byFirstBodyCell(parse(project, RULES));
        assertEquals(compiled.get("second"), test);
        assertEquals(compiled.get("String"), datatype);
        assertEquals(compiled.get("String"), TableIds.idOfWritten(new GridTable(1, 1, 2, 2, grid(project, file, 1)),
                null), "a table alone of its kind needs no module to be numbered in");
    }

    @Test
    void aWriteKeepsTheIdOfATableThatIsStillWhatItWas() throws IOException {
        var project = project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1"));
        var file = project.resolve(RULES);
        var rate = only(parse(project, RULES));
        var id = rate.getId();

        workbook(file, workbook -> put(workbook.createSheet("Rules"), 1, 1, "Rules String rate(String state)",
                "C1|RET1", "\"CA\"|1"));
        assertEquals(id, TableIds.idAfterWrite(rate, new GridTable(1, 1, 3, 2, grid(project, file))));

        workbook(file, workbook -> put(workbook.createSheet("Rules"), 1, 1, "Rules String price(String state)",
                "C1|RET1"));
        var renamed = TableIds.idAfterWrite(rate, new GridTable(1, 1, 2, 2, grid(project, file)));
        assertNotEquals(id, renamed);
        assertEquals(only(parse(project, RULES)).getId(), renamed);
    }

    @Test
    void aMemberIsNamedByTheTableItComesFrom() throws IOException {
        var table = only(parse(project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1")),
                RULES));
        var info = mock(IMemberMetaInfo.class);
        when(info.getSyntaxNode()).thenReturn(table);

        assertEquals(table.getId(), TableIds.idOf(info));
        assertNull(TableIds.idOf(mock(IMemberMetaInfo.class)), "a member no table holds");
        assertNull(TableIds.idOf((IMemberMetaInfo) null));
    }

    @Test
    void aTableOfNoModuleIsNamedAlone() throws Exception {
        var project = project("a", sheet -> put(sheet, 1, 1, "Rules String rate(String state)", "C1|RET1"));
        var grid = grid(project, project.resolve(RULES));
        var table = XlsHelper.createTableSyntaxNode(new GridTable(1, 1, 2, 2, grid), grid.getSheetSource());

        assertEquals(only(parse(project, RULES)).getId(), table.getId());
        assertNull(TableIds.moduleOf(table));
    }

    /** A workbook read on its own, with no project around it, as a test or a tool reads one, names its tables. */
    @Test
    void aTableOfAWorkbookOfNoProjectHasAnId() throws IOException {
        var file = workbook(dir.resolve("Alone.xlsx"),
                workbook -> put(workbook.createSheet("Rules"), 1, 1, "Rules String rate(String state)", "C1|RET1"));
        var module = (XlsModuleSyntaxNode) new SequentialXlsLoader().parse(new PathSourceCodeModule(file))
                .getTopNode();

        assertFalse(only(module).getId().isBlank());
    }

    private Path project(String name, Consumer<Sheet> rules) throws IOException {
        var project = dir.resolve(name);
        workbook(project.resolve(RULES), workbook -> rules.accept(workbook.createSheet("Rules")));
        return project;
    }

    private static Path workbook(Path file, Consumer<Workbook> content) throws IOException {
        Files.createDirectories(file.getParent());
        try (var workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            content.accept(workbook);
            workbook.write(out);
        }
        return file;
    }

    /** Writes a table from the given row and column: a line of cells separated by {@code |} per row. */
    private static void put(Sheet sheet, int row, int column, String... lines) {
        for (var i = 0; i < lines.length; i++) {
            var cells = lines[i].split("\\|");
            var line = sheet.getRow(row + i) == null ? sheet.createRow(row + i) : sheet.getRow(row + i);
            for (var j = 0; j < cells.length; j++) {
                line.createCell(column + j).setCellValue(cells[j]);
            }
        }
    }

    /** Writes a Markdown file holding one rule of two rows under the given header. */
    private static void markdown(Path file, String header) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                ---
                table: %s
                ---

                | Hour | Greeting |
                |---|---|
                | 0-11 | Good Morning |
                | 12-23 | Good Afternoon |
                """.formatted(header));
    }

    private static XlsModuleSyntaxNode parse(Path project, String path) {
        return (XlsModuleSyntaxNode) new SequentialXlsLoader().parse(new ProjectFile(project.resolve(path), project))
                .getTopNode();
    }

    private static XlsSheetGridModel grid(Path project, Path file) {
        return grid(project, file, 0);
    }

    private static XlsSheetGridModel grid(Path project, Path file, int sheet) {
        var workbook = new XlsWorkbookSourceCodeModule(new ProjectFile(file, project));
        return new XlsSheetGridModel(new XlsSheetSourceCodeModule(sheet, workbook));
    }

    private static TableSyntaxNode only(XlsModuleSyntaxNode module) {
        var tables = module.getXlsTableSyntaxNodes();
        assertEquals(1, tables.length);
        return tables[0];
    }

    private static Map<String, String> ids(XlsModuleSyntaxNode module) {
        var ids = new LinkedHashMap<String, String>();
        for (var table : module.getXlsTableSyntaxNodes()) {
            ids.put(table.getGridTable().getCell(0, 0).getStringValue(), table.getId());
        }
        return ids;
    }

    /** The ids of the tables by the first cell of their body, which tells apart tables sharing a header. */
    private static Map<String, String> byFirstBodyCell(XlsModuleSyntaxNode module) {
        var ids = new LinkedHashMap<String, String>();
        for (var table : module.getXlsTableSyntaxNodes()) {
            var grid = table.getGridTable();
            ids.put(grid.getCell(0, 1).getStringValue(), table.getId());
        }
        return ids;
    }

    /** A workbook of a project, as OpenL Studio compiles a module. */
    private static final class ProjectFile extends PathSourceCodeModule implements IProjectSourceCodeModule {

        @Getter
        private final Path projectFolder;

        ProjectFile(Path file, Path projectFolder) {
            super(file);
            this.projectFolder = projectFolder;
        }
    }
}
