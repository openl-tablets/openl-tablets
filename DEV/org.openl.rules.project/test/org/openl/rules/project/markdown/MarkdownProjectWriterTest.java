package org.openl.rules.project.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;

/**
 * A copy of a project with its workbooks written as module folders of Markdown files compiles to the same rules: its
 * {@code rules.xml} names the folders, and a workbook a Markdown file cannot hold stays as it is.
 */
class MarkdownProjectWriterTest {

    /** The module the wildcard of {@link #projectWithWorkbookInItsBuild} finds in the build folder. */
    private static final MarkdownProjectWriter.Dropped COMMON = new MarkdownProjectWriter.Dropped("Common",
            "build/openl-workspace/Common/rules/Common.xlsx");

    @TempDir
    Path dir;

    @Test
    void copyOfAProjectNamesTheFoldersOfItsWorkbooksAndTestsAlike() throws Exception {
        var project = Files.createDirectories(dir.resolve("project"));
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Rating</name>
                    <modules>
                        <module>
                            <name>Rates</name>
                            <rules-root path="rules/Rates.xlsx"/>
                        </module>
                        <module>
                            <name>Tests</name>
                            <rules-root path="rules/Tests.xlsx"/>
                        </module>
                        <module>
                            <name>Algorithms</name>
                            <rules-root path="rules/Algorithms.xlsx"/>
                        </module>
                    </modules>
                </project>
                """);
        workbook(project.resolve("rules/Rates.xlsx"), "Rates", sheet -> {
            row(sheet, 0, "SmartRules Double rate(Integer age)");
            row(sheet, 1, "Age", "Rate");
            row(sheet, 2, "0-17", 1.5);
            row(sheet, 3, "18-120", 1.0);
        });
        workbook(project.resolve("rules/Tests.xlsx"), "Tests", sheet -> {
            row(sheet, 0, "Environment");
            row(sheet, 1, "dependency", "Rates");
            row(sheet, 3, "Test rate rateTest");
            row(sheet, 4, "age", "_res_");
            row(sheet, 5, "Age", "Rate");
            row(sheet, 6, 10.0, 1.5);
            row(sheet, 7, 40.0, 1.0);
        });
        algorithms(project.resolve("rules/Algorithms.xlsx"));
        var copy = dir.resolve("copy");

        var written = MarkdownProjectWriter.write(factory(project), copy, List.of());

        assertEquals(List.of("Rates rules/Rates.xlsx -> rules/Rates", "Tests rules/Tests.xlsx -> rules/Tests"),
                written.converted().stream()
                        .map(module -> module.name() + " " + module.workbook() + " -> " + module.folder())
                        .toList());
        assertEquals(List.of("""
                        Algorithms rules/Algorithms.xlsx: Method Integer three(): Only the header and the \
                        properties stand above the body, but B1 holds a value"""),
                written.kept().stream()
                        .map(module -> module.name() + " " + module.path() + ": " + String.join("; ", module.reasons()))
                        .toList());
        assertTrue(Files.isRegularFile(copy.resolve("rules/Rates/rate.md")));
        assertFalse(Files.exists(copy.resolve("rules/Rates.xlsx")));
        assertTrue(Files.isRegularFile(copy.resolve("rules/Algorithms.xlsx")));
        assertEquals("Rating", Objects.requireNonNull(ProjectDescriptor.read(copy)).getName());
        assertEquals(List.of("rules/Rates", "rules/Tests", "rules/Algorithms.xlsx"), modulePaths(copy));
        assertEquals(List.of("rateTest: 2 units, 0 failed"), tests(copy));
        assertEquals(tests(project), tests(copy));
    }

    @Test
    void workbookAnotherModuleIncludesStaysAWorkbook() throws Exception {
        var project = Files.createDirectories(dir.resolve("project"));
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Common</name>
                    <modules>
                        <module>
                            <name>Common</name>
                            <rules-root path="Common.xlsx"/>
                        </module>
                        <module>
                            <name>Main</name>
                            <rules-root path="Main.xlsx"/>
                        </module>
                    </modules>
                </project>
                """);
        workbook(project.resolve("Common.xlsx"), "Common", sheet -> {
            row(sheet, 0, "Method Integer one()");
            row(sheet, 1, "return 1;");
        });
        workbook(project.resolve("Main.xlsx"), "Main", sheet -> {
            row(sheet, 0, "Environment");
            row(sheet, 1, "include", "Common.xlsx");
            row(sheet, 3, "Method Integer two()");
            row(sheet, 4, "return one() + 1;");
        });
        var copy = dir.resolve("copy");

        var written = MarkdownProjectWriter.write(factory(project), copy, List.of());

        assertEquals(List.of("Common: Another module includes the workbook",
                "Main: The workbook includes Common.xlsx, the workbook of another module"), written.kept().stream()
                .map(module -> module.name() + ": " + String.join("; ", module.reasons()))
                .toList());
        assertTrue(Files.isRegularFile(copy.resolve("Common.xlsx")));
        assertTrue(Files.isRegularFile(copy.resolve("Main.xlsx")));
        // The copy compiles as the project does
        assertEquals(errorsOf(project), errorsOf(copy));
    }

    @Test
    void copyLeavesOutTheFilesAndFoldersItIsGiven() throws Exception {
        var project = ratesProject(dir.resolve("project"));
        Files.writeString(project.resolve("README.txt"), "The rates");
        var pom = Files.writeString(project.resolve("pom.xml"), "<project/>");
        // A build folder named otherwise than target, holding the copy and the projects the build depends on
        var build = project.resolve("build");
        Files.writeString(Files.createDirectories(build.resolve("openl-workspace/Common")).resolve("rules.xml"),
                "<project/>");
        var copy = build.resolve("openl-markdown");

        MarkdownProjectWriter.write(factory(project), copy, List.of(build, pom, dir.resolve("elsewhere")));

        try (var files = Files.list(copy)) {
            var names = files.map(file -> file.getFileName().toString())
                    .sorted()
                    .toList();
            assertEquals(List.of("README.txt", "rules", "rules.xml"), names);
        }
        assertTrue(Files.isRegularFile(copy.resolve("rules/Rates/rate.md")));
    }

    @Test
    void moduleOfAFolderLeftOutIsNoModuleOfTheCopy() throws Exception {
        var project = projectWithWorkbookInItsBuild();
        rates(project.resolve("rules/Rates.xlsx"));
        var copy = project.resolve("build/openl-markdown");

        var written = MarkdownProjectWriter.write(factory(project), copy, List.of(project.resolve("build")));

        assertEquals(List.of("rules/Rates.xlsx -> rules/Rates"), written.converted()
                .stream()
                .map(module -> module.workbook() + " -> " + module.folder())
                .toList());
        assertEquals(List.of(), written.kept());
        assertEquals(List.of(COMMON), written.dropped());
        assertFalse(Files.exists(copy.resolve("build")));
        assertEquals(List.of("rules/Rates"), modulePaths(copy));
        var compiled = factory(copy).getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
    }

    /** The folder left out is named through a link, as a build folder Maven names while the sources are resolved. */
    @DisabledOnOs(OS.WINDOWS)
    @Test
    void folderLeftOutThroughALinkIsLeftOut() throws Exception {
        var project = projectWithWorkbookInItsBuild();
        rates(project.resolve("rules/Rates.xlsx"));
        var link = Files.createSymbolicLink(dir.resolve("link"), project);
        var copy = project.resolve("build/openl-markdown");

        var written = MarkdownProjectWriter.write(factory(project), copy, List.of(link.resolve("build")));

        assertEquals(List.of(COMMON), written.dropped());
        assertFalse(Files.exists(copy.resolve("build")));
        assertEquals(List.of("rules/Rates"), modulePaths(copy));
    }

    @Test
    void copyNamesNoModuleItDropsWhenItConvertsNone() throws Exception {
        var project = projectWithWorkbookInItsBuild();
        algorithms(project.resolve("rules/Algorithms.xlsx"));
        var copy = project.resolve("build/openl-markdown");

        var written = MarkdownProjectWriter.write(factory(project), copy, List.of(project.resolve("build")));

        assertEquals(List.of(), written.converted());
        assertEquals(List.of("Algorithms"), written.kept()
                .stream()
                .map(MarkdownProjectWriter.Kept::name)
                .toList());
        assertEquals(List.of(COMMON), written.dropped());
        assertEquals(List.of("rules/Algorithms.xlsx"), modulePaths(copy));
    }

    /** The sources are built into the build folder, which therefore holds the project: nothing is left out. */
    @Test
    void folderHoldingTheProjectLeavesNothingOut() throws Exception {
        var build = dir.resolve("build");
        var project = ratesProject(build.resolve("openl"));
        var copy = build.resolve("openl-markdown");

        var written = MarkdownProjectWriter.write(factory(project), copy, List.of(build, project));

        assertEquals(List.of("Rates"), written.converted()
                .stream()
                .map(MarkdownProjectWriter.Converted::name)
                .toList());
        assertEquals(List.of(), written.dropped());
        assertTrue(Files.isRegularFile(copy.resolve("rules/Rates/rate.md")));
    }

    /** A second build without a clean finds the workbook its first copy kept, which goes with that copy. */
    @Test
    void moduleOfAnEarlierCopyIsNoModuleOfTheCopy() throws Exception {
        var project = projectWithWorkbookInItsBuild();
        rates(project.resolve("rules/Rates.xlsx"));
        // A copy in the sources, out of the build folder
        var copy = project.resolve("markdown");
        var build = List.of(project.resolve("build"));
        MarkdownProjectWriter.write(factory(project), copy, build);
        workbook(copy.resolve("rules/Stale.xlsx"), "Stale", sheet -> {
            row(sheet, 0, "Method Integer four()");
            row(sheet, 1, "return 4;");
        });

        var written = MarkdownProjectWriter.write(factory(project), copy, build);

        assertEquals(List.of(COMMON, new MarkdownProjectWriter.Dropped("Stale", "markdown/rules/Stale.xlsx")),
                written.dropped());
        assertFalse(Files.exists(copy.resolve("rules/Stale.xlsx")));
        assertEquals(List.of("rules/Rates"), modulePaths(copy));
    }

    /**
     * A project with a module path matching any workbook, a build folder among them: the project a build depends on,
     * extracted into the build folder.
     */
    private Path projectWithWorkbookInItsBuild() throws IOException {
        var project = Files.createDirectories(dir.resolve("project"));
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Rating</name>
                    <modules>
                        <module>
                            <rules-root path="**/*.xlsx"/>
                        </module>
                    </modules>
                </project>
                """);
        workbook(project.resolve("build/openl-workspace/Common/rules/Common.xlsx"), "Common", sheet -> {
            row(sheet, 0, "Method Integer one()");
            row(sheet, 1, "return 1;");
        });
        return project;
    }

    /** A project of the one module Rates, named in its {@code rules.xml}. */
    private static Path ratesProject(Path folder) throws IOException {
        var project = Files.createDirectories(folder);
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Rating</name>
                    <modules>
                        <module>
                            <name>Rates</name>
                            <rules-root path="rules/Rates.xlsx"/>
                        </module>
                    </modules>
                </project>
                """);
        rates(project.resolve("rules/Rates.xlsx"));
        return project;
    }

    /** A workbook a Markdown file cannot hold: a note stands beside the header. */
    private static void algorithms(Path file) throws IOException {
        workbook(file, "Algorithms", sheet -> {
            row(sheet, 0, "Method Integer three()", "// a note beside the header");
            row(sheet, 1, "return 3;");
        });
    }

    private static void rates(Path file) throws IOException {
        workbook(file, "Rates", sheet -> {
            row(sheet, 0, "SmartRules Double rate(Integer age)");
            row(sheet, 1, "Age", "Rate");
            row(sheet, 2, "0-120", 1.0);
        });
    }

    private static List<String> modulePaths(Path project) {
        return Objects.requireNonNull(ProjectDescriptor.read(project))
                .getModules()
                .stream()
                .map(Module::getRulesRootPath)
                .toList();
    }

    private static List<String> errorsOf(Path project) throws Exception {
        return factory(project).getCompiledOpenClass()
                .getAllMessages()
                .stream()
                .map(message -> message.getSeverity() + ": " + message.getSummary())
                .toList();
    }

    private static SimpleProjectEngineFactory<Object> factory(Path project) {
        return new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject(project.toString())
                .setExecutionMode(false)
                .build();
    }

    /** Each test of a project with the number of its test units and of their failures. */
    private static List<String> tests(Path project) throws Exception {
        var compiled = factory(project).getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        var openClass = compiled.getOpenClass();
        var env = new SimpleRulesVM().getRuntimeEnv();
        var instance = openClass.newInstance(env);
        return Arrays.stream(ProjectHelper.allTesters(openClass)).map(test -> {
            var result = (TestUnitsResults) test.invoke(instance, new Object[0], env);
            return "%s: %d units, %d failed".formatted(test.getName(), result.getNumberOfTestUnits(),
                    result.getNumberOfFailures());
        }).toList();
    }

    private static void workbook(Path file, String sheetName, SheetContent content) throws IOException {
        Files.createDirectories(file.getParent());
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            content.fill(workbook.createSheet(sheetName));
            workbook.write(out);
        }
    }

    private static void row(Sheet sheet, int index, Object... values) {
        var row = sheet.createRow(index);
        for (var column = 0; column < values.length; column++) {
            if (values[column] instanceof Double number) {
                row.createCell(column).setCellValue(number);
            } else {
                row.createCell(column).setCellValue(values[column].toString());
            }
        }
    }

    @FunctionalInterface
    private interface SheetContent {
        void fill(Sheet sheet);
    }
}
