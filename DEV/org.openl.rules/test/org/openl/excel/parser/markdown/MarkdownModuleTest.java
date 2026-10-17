package org.openl.excel.parser.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.TestUtils;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.impl.PathSourceCodeModule;

/**
 * A module written as a folder of Markdown files, a table to a file, compiles into the same tables an Excel workbook
 * gives, and runs the same way.
 */
class MarkdownModuleTest {

    private static final String GREETINGS = "test/rules/markdown/Greetings";
    private static final String BROKEN = "test/rules/markdown/Broken";

    private static CompiledOpenClass compiled;
    private static Object rules;

    @BeforeAll
    static void compile() {
        var factory = new RulesEngineFactory<>(GREETINGS);
        compiled = factory.getCompiledOpenClass();
        rules = factory.newEngineInstance();
    }

    @Test
    void compilesWithoutErrors() {
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
    }

    @Test
    void rulesRun() {
        assertEquals("Good Morning", TestUtils.invoke(rules, "greeting", 9));
        assertEquals("Good Evening", TestUtils.invoke(rules, "greeting", 20));
        assertEquals("Hello, Ann", TestUtils.invoke(rules, "hello", "Ann"));
        assertEquals(1200.0, TestUtils.<Double>invoke(rules, "basePremium", 18));
        assertEquals(900.0, TestUtils.<Double>invoke(rules, "basePremium", 40));
    }

    @Test
    void tablesOfTheFilesSeeEachOther() {
        Object[] drivers = TestUtils.invoke(rules, "getDrivers");

        SpreadsheetResult young = TestUtils.invoke(rules, "premium", drivers[0]);
        SpreadsheetResult adult = TestUtils.invoke(rules, "premium", drivers[1]);

        assertEquals(1800.0, young.getFieldValue("$Value$Total"));
        assertEquals(900.0, adult.getFieldValue("$Value$Total"));
    }

    @Test
    void subFoldersGroupTheTables() {
        var greeting = table("SmartRules String greeting");

        assertEquals("Greeting by the hour of the day", greeting.getTableProperties().getDescription());
        assertEquals("Greetings", greeting.getXlsSheetSourceCodeModule().getSheetName());
        assertEquals("Premium", table("Datatype Driver").getXlsSheetSourceCodeModule().getSheetName());
    }

    @Test
    void tableIsKnownByItsFileWhateverTheOtherFiles() {
        // A file holds one table, at the top left corner of its sheet
        var greeting = table("SmartRules String greeting").getUri();
        var driver = table("Datatype Driver").getUri();

        assertTrue(greeting.endsWith("Greetings/greeting.md?sheet=Greetings&range=A1:C6"), greeting);
        assertTrue(driver.endsWith("Greetings/Premium/Driver.md?sheet=Premium&range=A1:B4"), driver);
    }

    @Test
    void cellsHaveTheDefaultLook() {
        var cell = table("SmartRules String greeting").getGridTable().getCell(0, 0);

        assertNull(cell.getFont());
        assertNull(cell.getComment());
        assertNull(cell.getFormula());
    }

    @Test
    void tablesOfTheModuleAreWrittenInFilesOfTheirOwn() {
        var errors = errorsOf("test/rules/markdown/Misplaced");

        assertEquals(List.of(
                "Constants.md holds the Constants table of the module and nothing else",
                "The Environment table of a module is written in Environment.md at the root of the module folder"),
                errors.stream().sorted().toList());
    }

    @Test
    void formatErrorOfTheBodyKeepsTheTableAndPointsAtItsCell() {
        var module = new RulesEngineFactory<>(BROKEN).getCompiledOpenClass();

        var error = errorOf(module, "broken.md");
        assertEquals("The merged range A1:C1 is outside the table", error.getSummary());
        // The range starts in the first cell of the body, which lies under the header
        assertTrue(error.getSourceLocation().endsWith("Broken/broken.md?sheet=Broken&cell=A2"),
                error::getSourceLocation);
        assertTrue(hasMethod(module, "broken"));
    }

    @Test
    void formatErrorOfTheFrontMatterLosesTheTableOfItsFileOnly() throws IOException {
        var module = new RulesEngineFactory<>(BROKEN).getCompiledOpenClass();

        // The error tells what is wrong, as an error of a workbook does, and points at the line of the file
        var error = errorOf(module, "Folder/unread.md");
        assertEquals("A value of the front matter must be text or a list", error.getSummary());
        var text = Files.readString(Path.of(BROKEN, "Folder/unread.md"));
        var line = text.indexOf("description:");
        assertTrue(error.getSourceLocation().endsWith("Broken/Folder/unread.md?start=%d&end=%d".formatted(line,
                line + "description:".length() - 1)), error::getSourceLocation);
        // The unreadable file stays in the group of its folder
        var sheets = Arrays.stream(((XlsMetaInfo) module.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                        .getWorkbookSyntaxNodes())
                .flatMap(workbook -> Arrays.stream(workbook.getWorksheetSyntaxNodes()))
                .map(sheet -> ((XlsSheetSourceCodeModule) sheet.getModule()).getSheetName())
                .distinct()
                .toList();
        assertEquals(List.of("Folder", "Broken"), sheets);
        // The other files compile
        assertFalse(hasMethod(module, "unread"));
        assertTrue(hasMethod(module, "hello"));
        assertEquals(2, errorsOf(module).size(), () -> errorsOf(module).toString());
    }

    @Test
    void errorOfAFileReadOnItsOwnTellsWhatIsWrong(@TempDir Path dir) throws IOException {
        var file = dir.resolve("My Rules/alone.md");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "---\ndescription: x\n---\n\n| a |\n|---|\n");

        assertEquals(List.of("The front matter has no 'table' key with the header of the table"),
                errorsOf(file.toString()));
    }

    @Test
    void fileIncludedFromItsOwnModuleIsReadOnce() {
        var module = new RulesEngineFactory<>("test/rules/markdown/Including").getCompiledOpenClass();

        // The walk of the folder and the includes of the file and of its folder name the file by different
        // addresses: it is one file all the same
        assertEquals(List.of(), errorsOf(module));
        assertEquals(1, module.getOpenClassWithErrors()
                .getMethods()
                .stream()
                .filter(method -> method.getName().equals("hi"))
                .count());
    }

    @Test
    void filesAreReadInTheOrderOfTheirNamesANumberByItsValue(@TempDir Path dir) throws IOException {
        var table = """
                ---
                table: Method Integer rate()
                ---

                ```java
                return 1;
                ```
                """;
        Files.writeString(dir.resolve("rate10.md"), table);
        Files.writeString(dir.resolve("rate2.md"), table);

        // The second table of the same method is the one read later: rate10.md comes after rate2.md
        var duplicates = new RulesEngineFactory<>(dir.toString()).getCompiledOpenClass().getAllMessages().stream()
                .filter(message -> message.getSeverity() == Severity.ERROR)
                .map(OpenLMessage::getSourceLocation)
                .toList();
        assertEquals(1, duplicates.size(), duplicates::toString);
        assertTrue(duplicates.getFirst().contains("/rate10.md?"), duplicates::toString);
    }

    @Test
    void emptyModuleFolderIsAnError(@TempDir Path dir) {
        assertEquals(List.of("The module folder holds no Markdown file."), errorsOf(dir.toString()));
    }

    @Test
    void fileReadFromAnArchiveNamesItsSheetAfterItsFolder(@TempDir Path dir) throws Exception {
        var archive = dir.resolve("project.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("rules/My Rules/hi.md"));
            zip.write("---\ntable: Method String hi()\n---\n\n```java\nreturn \"Hi\";\n```\n"
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var url = URI.create("jar:" + archive.toUri() + "!/rules/My%20Rules/hi.md").toURL();

        var module = new RulesEngineFactory<>(url).getCompiledOpenClass();

        // Read from the archive as a stream, the file is known by its address, written with %20 for the space
        var node = ((XlsMetaInfo) module.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                .getXlsTableSyntaxNodes()[0];
        assertEquals("My Rules", node.getXlsSheetSourceCodeModule().getSheetName());
    }

    @Test
    void moduleFolderReadInsideAnArchiveKnowsItsFilesUnderItsAddress(@TempDir Path dir) throws Exception {
        var archive = dir.resolve("project.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("rules/My Rules/hi.md"));
            zip.write("---\ntable: Method String hi()\n---\n\n```java\nreturn \"Hi\";\n```\n"
                    .getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new ZipEntry("rules/My Rules/Sub Folder/hey.md"));
            zip.write("---\ntable: Method String hey()\n---\n\n```java\nreturn \"Hey\";\n```\n"
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        // Rule Services reads a deployed archive in place, through the file system of the archive
        try (var fs = FileSystems.newFileSystem(URI.create("jar:" + archive.toUri()), Map.of())) {
            var module = new PathSourceCodeModule(fs.getPath("/rules/My Rules"));

            var read = new RulesEngineFactory<>(module).getCompiledOpenClass();

            assertEquals(List.of(), errorsOf(read));
            var workbooks = Arrays.stream(((XlsMetaInfo) read.getOpenClassWithErrors().getMetaInfo())
                            .getXlsModuleNode()
                            .getWorkbookSyntaxNodes())
                    .map(workbook -> workbook.getWorkbookSourceCodeModule().getUri())
                    .toList();
            var base = module.getUri().endsWith("/") ? module.getUri() : module.getUri() + "/";
            assertEquals(List.of(base + "Sub%20Folder/hey.md", base + "hi.md"), workbooks);
        }
    }

    /** The error whose address points into the given file of the module folder. */
    private static OpenLMessage errorOf(CompiledOpenClass module, String file) {
        return module.getAllMessages()
                .stream()
                .filter(message -> message.getSeverity() == Severity.ERROR)
                .filter(message -> message.getSourceLocation() != null
                        && message.getSourceLocation().contains("/Broken/" + file + "?"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(module.getAllMessages().toString()));
    }

    private static boolean hasMethod(CompiledOpenClass module, String name) {
        return module.getOpenClassWithErrors().getMethods().stream().anyMatch(method -> method.getName().equals(name));
    }

    private static List<String> errorsOf(String module) {
        return errorsOf(new RulesEngineFactory<>(module).getCompiledOpenClass());
    }

    private static List<String> errorsOf(CompiledOpenClass module) {
        return module.getAllMessages()
                .stream()
                .filter(message -> message.getSeverity() == Severity.ERROR)
                .map(OpenLMessage::getSummary)
                .toList();
    }

    private static TableSyntaxNode table(String headerStart) {
        var nodes = ((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                .getXlsTableSyntaxNodes();
        return Arrays.stream(nodes)
                .filter(node -> node.getHeader().getSourceString().startsWith(headerStart))
                .findFirst()
                .orElseThrow();
    }
}
