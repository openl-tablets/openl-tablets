package org.openl.rules.project.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
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
        workbook(project.resolve("rules/Algorithms.xlsx"), "Algorithms", sheet -> {
            row(sheet, 0, "Method Integer three()", "a note beside the header");
            row(sheet, 1, "return 3;");
        });
        var copy = dir.resolve("copy");

        var written = MarkdownProjectWriter.write(factory(project), copy);

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
        var descriptor = ProjectDescriptor.read(copy);
        assertEquals("Rating", descriptor.getName());
        assertEquals(List.of("rules/Rates", "rules/Tests", "rules/Algorithms.xlsx"),
                descriptor.getModules().stream().map(Module::getRulesRootPath).toList());
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

        var written = MarkdownProjectWriter.write(factory(project), copy);

        assertEquals(List.of("Common: Another module includes the workbook",
                "Main: The workbook includes Common.xlsx, the workbook of another module"), written.kept().stream()
                .map(module -> module.name() + ": " + String.join("; ", module.reasons()))
                .toList());
        assertTrue(Files.isRegularFile(copy.resolve("Common.xlsx")));
        assertTrue(Files.isRegularFile(copy.resolve("Main.xlsx")));
        // The copy compiles as the project does
        assertEquals(errorsOf(project), errorsOf(copy));
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
