package org.openl.excel.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.TestUtils;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenField;

/**
 * The tables of a workbook written into a module folder of Markdown files are compiled back into the same tables, a
 * table to a file, on sheets of the same names.
 */
class MarkdownModuleWriterTest {

    @TempDir
    Path dir;

    @Test
    void tablesOfAWorkbookAreReadBackFromTheirFiles() throws IOException {
        var workbook = workbook("Rules.xlsx", rules -> {
            var rates = rules.createSheet("Rates");
            row(rates, 0, "SmartRules Double rate(Integer age, String state)");
            row(rates, 1, "properties", "description", "Rates by age");
            row(rates, 2, "Age", "State", "Rate");
            row(rates, 3, "0-17", "AL", 0.1);
            row(rates, 4, null, "AK", 0.2);
            row(rates, 5, "18-120", "AL", 0.3);
            rates.addMergedRegion(new CellRangeAddress(3, 4, 0, 0));
            var model = rules.createSheet("Model");
            row(model, 0, "Datatype Driver");
            row(model, 1, "Integer", "age");
            row(model, 2, "Date", "licensed");
            row(model, 4, "Data Driver drivers");
            row(model, 5, "age", "licensed");
            row(model, 6, "Age", "Licensed");
            row(model, 7, 30.0, date(2010, 5, 1));
            row(model, 8, 40.0, 40299.0);
            // A Datatype that adds no field to its parent is its header alone
            row(model, 10, "Datatype Adult extends Driver");
            var constants = rules.createSheet("Rules");
            row(constants, 0, "Constants");
            row(constants, 1, "Integer", "MAX_AGE", 120.0);
        });
        var module = dir.resolve("md/Rules");

        var written = MarkdownModuleWriter.write(workbookNode(workbook), module);

        assertEquals(List.of(), written.leftOut());
        assertEquals(List.of("Rates/rate.md", "Model/Driver.md", "Model/drivers.md", "Model/Adult.md", "Constants.md"),
                written.files().stream().map(file -> module.relativize(file).toString().replace('\\', '/')).toList());
        assertEquals("""
                ---
                table: SmartRules Double rate(Integer age, String state)
                properties:
                  description: Rates by age
                merges:
                  - A2:A3
                ---

                | Age | State | Rate |
                |---|---|---|
                | 0-17 | AL | 0.1 |
                |  | AK | 0.2 |
                | 18-120 | AL | 0.3 |
                """, Files.readString(module.resolve("Rates/rate.md")));
        assertEquals("---\ntable: Datatype Adult extends Driver\n---\n",
                Files.readString(module.resolve("Model/Adult.md")));
        var factory = new RulesEngineFactory<>(module.toString());
        var compiled = factory.getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        var rules = factory.newEngineInstance();
        assertEquals(0.2, TestUtils.<Double>invoke(rules, "rate", 10, "AK"));
        assertEquals(0.3, TestUtils.<Double>invoke(rules, "rate", 40, "AL"));
        Object[] drivers = TestUtils.invoke(rules, "getDrivers");
        assertEquals(date(2010, 5, 1), TestUtils.invoke(drivers[0], "getLicensed"));
        // A number the compiler read as a date is written as the date
        assertEquals(date(2010, 5, 1), TestUtils.invoke(drivers[1], "getLicensed"));
    }

    @Test
    void tableAFileCannotHoldIsLeftOutWithTheReason() throws IOException {
        var workbook = workbook("Algorithms.xlsx", rules -> {
            var sheet = rules.createSheet("Algorithms");
            row(sheet, 4, "Properties");
            row(sheet, 5, "scope", "Module");
            row(sheet, 7, "Properties");
            row(sheet, 8, "scope", "Global");
            row(sheet, 10, "Method Integer three()", "a note beside the header");
            row(sheet, 11, "return 3;");
            row(sheet, 13, "A note: the algorithms count to three");
        });
        var module = dir.resolve("md/Algorithms");

        var written = MarkdownModuleWriter.write(workbookNode(workbook), module);

        // A module holds a Properties table of each scope, each a table like any other
        assertEquals(List.of(module.resolve("Properties.md"), module.resolve("Properties1.md"),
                module.resolve("notes.md")), written.files());
        assertEquals("# Notes\n\nA note: the algorithms count to three\n\n",
                Files.readString(module.resolve("notes.md")));
        assertEquals(List.of(
                """
                Method Integer three(): Only the header and the properties stand above the body, but B1 holds a \
                value"""),
                written.leftOut()
                        .stream()
                        .map(table -> table.header() + ": " + table.reason())
                        .toList());
    }

    @Test
    void tableWrittenInPartsIsWrittenWholeAsTheCompilerJoinedIt() throws IOException {
        var workbook = workbook("Parts.xlsx", rules -> {
            var rates = rules.createSheet("Rates");
            row(rates, 0, "TablePart Rates row 1 of 2");
            row(rates, 1, "SmartRules Double rate(Integer age)");
            row(rates, 2, "Age", "Rate");
            row(rates, 3, "< 25", 1.5);
            rates.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));
            // Parts side by side: the header of each part repeats the header of the table
            row(rates, 5, "TablePart Zones column 1 of 2", null, "TablePart Zones column 2 of 2");
            row(rates, 6, "SmartRules String zone(String zip)", null, "SmartRules String zone(String zip)");
            row(rates, 7, "Zip", null, "Zone");
            row(rates, 8, "10001", null, "East");
            row(rates, 9, "90210", null, "West");
            var more = rules.createSheet("More");
            row(more, 0, "TablePart Rates row 2 of 2");
            row(more, 1, "25 - 64", 1.0);
            row(more, 2, ">= 65", 1.2);
        });
        var module = dir.resolve("md/Parts");

        var written = MarkdownModuleWriter.write(workbookNode(workbook), module);

        // The parts are not written: the table they make is, in the folder of the sheet of its first part
        assertEquals(List.of(), written.leftOut());
        assertEquals(List.of("Rates/rate.md", "Rates/zone.md"),
                written.files().stream().map(file -> module.relativize(file).toString().replace('\\', '/'))
                        .sorted()
                        .toList());
        assertEquals("""
                ---
                table: SmartRules Double rate(Integer age)
                ---

                | Age | Rate |
                |---|---|
                | < 25 | 1.5 |
                | 25 - 64 | 1 |
                | >= 65 | 1.2 |
                """, Files.readString(module.resolve("Rates/rate.md")));
        assertEquals("""
                | Zip | Zone |
                |---|---|
                | 10001 | East |
                | 90210 | West |
                """, bodyOf(module.resolve("Rates/zone.md")));
        var factory = new RulesEngineFactory<>(module.toString());
        var compiled = factory.getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        var rules = factory.newEngineInstance();
        assertEquals(1.2, TestUtils.<Double>invoke(rules, "rate", 70));
        assertEquals("West", TestUtils.invoke(rules, "zone", "90210"));
    }

    @Test
    void partsThatDoNotMakeATableAreLeftOut() throws IOException {
        var workbook = workbook("Lost.xlsx", rules -> {
            var rates = rules.createSheet("Rates");
            row(rates, 0, "TablePart Rates row 1 of 2");
            row(rates, 1, "SmartRules Double rate(Integer age)");
            row(rates, 2, "Age", "Rate");
            row(rates, 3, "< 25", 1.5);
            rates.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));
        });

        var written = MarkdownModuleWriter.write(workbookNode(workbook), dir.resolve("md/Lost"));

        // Without its second part the table is not joined, and its first part would be lost with it
        assertEquals(List.of("TablePart Rates row 1 of 2: The parts of a table could not be joined into the table"),
                written.leftOut().stream().map(table -> table.header() + ": " + table.reason()).toList());
    }

    @Test
    void indentThatBuildsATBasicTableIsWrittenAsNonBreakingSpaces() throws IOException {
        var workbook = workbook("Bonus.xlsx", rules -> {
            var sheet = rules.createSheet("Bonus");
            row(sheet, 0, "TBasic Integer bonus(Integer age)");
            row(sheet, 1, "Description", "Operation", "Condition", "Action");
            row(sheet, 2, "Description", "Operation", "Condition", "Action");
            row(sheet, 3, "Young", "IF", "age < 25");
            row(sheet, 4, "Bonus", "RETURN", "10");
            row(sheet, 5, "Default", "RETURN", "0");
            // The return of the bonus is a step of the IF: without the indent it would be returned whatever the age
            var indented = rules.createCellStyle();
            indented.setIndention((short) 1);
            sheet.getRow(4).getCell(1).setCellStyle(indented);
        });
        var module = dir.resolve("md/Bonus");

        var written = MarkdownModuleWriter.write(workbookNode(workbook), module);

        assertEquals(List.of(), written.leftOut());
        assertEquals("""
                ---
                table: TBasic Integer bonus(Integer age)
                ---

                | Description | Operation | Condition | Action |
                |---|---|---|---|
                | Description | Operation | Condition | Action |
                | Young | IF | age < 25 |  |
                | Bonus | &nbsp;&nbsp;RETURN | 10 |  |
                | Default | RETURN | 0 |  |
                """, Files.readString(module.resolve("bonus.md")));
        var factory = new RulesEngineFactory<>(module.toString());
        assertFalse(factory.getCompiledOpenClass().hasErrors(), () -> factory.getCompiledOpenClass().getAllMessages()
                .toString());
        var rules = factory.newEngineInstance();
        assertEquals(10, TestUtils.<Integer>invoke(rules, "bonus", 20));
        assertEquals(0, TestUtils.<Integer>invoke(rules, "bonus", 40));
    }

    @Test
    void environmentAndConstantsTablesOfAWorkbookAreJoinedInTheFileOfTheModule() throws IOException {
        workbook("Common.xlsx", rules -> row(rules.createSheet("Common"), 0, "Method Integer one()")
                .getSheet().createRow(1).createCell(0).setCellValue("return 1;"));
        var workbook = workbook("Main.xlsx", rules -> {
            var main = rules.createSheet("Main");
            row(main, 0, "Environment");
            row(main, 1, "import", "java.time");
            row(main, 3, "Constants");
            row(main, 4, "Integer", "ONE", 1.0);
            row(main, 6, "Method Integer three()");
            row(main, 7, "return one() + ONE + TWO;");
            var trace = rules.createSheet("Trace");
            row(trace, 0, "Environment");
            row(trace, 1, "include", "Common.xlsx");
            row(trace, 3, "Constants");
            row(trace, 4, "Integer", "TWO", 2.0);
        });
        var module = dir.resolve("Main");

        var written = MarkdownModuleWriter.write(workbookNode(workbook), module);

        assertEquals(List.of(), written.leftOut());
        // The rows of each table follow the rows of the one before, as the engine reads them
        assertEquals("""
                ---
                table: Environment
                ---

                | import | java.time |
                |---|---|
                | include | ../Common.xlsx |
                """, Files.readString(module.resolve("Environment.md")));
        assertEquals("""
                ---
                table: Constants
                ---

                | Integer | ONE | 1 |
                |---|---|---|
                | Integer | TWO | 2 |
                """, Files.readString(module.resolve("Constants.md")));
        var factory = new RulesEngineFactory<>(module.toString());
        assertFalse(factory.getCompiledOpenClass().hasErrors(), () -> factory.getCompiledOpenClass().getAllMessages()
                .toString());
        assertEquals(4, TestUtils.<Integer>invoke(factory.newEngineInstance(), "three"));
    }

    @Test
    void propertiesTableOfACategoryKeepsItsCategoryInTheFolderOfItsSheet() throws IOException {
        var workbook = workbook("Rating.xlsx", rules -> {
            var young = rules.createSheet("Young");
            row(young, 0, "Properties");
            row(young, 1, "scope", "Category");
            row(young, 2, "lob", "young");
            row(young, 4, "Method String youngRate()");
            row(young, 5, "return \"young\";");
            var adult = rules.createSheet("Adult");
            row(adult, 0, "Properties");
            row(adult, 1, "scope", "Module");
            row(adult, 2, "lob", "adult");
            row(adult, 4, "Method String adultRate()");
            row(adult, 5, "return \"adult\";");
        });
        var module = dir.resolve("Rating");

        var written = MarkdownModuleWriter.write(workbookNode(workbook), module);

        assertEquals(List.of("Young/Properties.md", "Young/youngRate.md", "Adult/Properties.md", "Adult/adultRate.md"),
                written.files().stream().map(file -> module.relativize(file).toString().replace('\\', '/')).toList());
        var compiled = new RulesEngineFactory<>(module.toString()).getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        // The category is the sheet, which is the folder: the module properties reach every table
        var lobs = Arrays.stream(((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                        .getXlsTableSyntaxNodes())
                .filter(table -> table.getMember() != null && table.getMember().getName().endsWith("Rate"))
                .map(table -> table.getMember().getName() + "=" + String.join(",", table.getTableProperties().getLob()))
                .sorted()
                .toList();
        assertEquals(List.of("adultRate=adult", "youngRate=young"), lobs);
    }

    @Test
    void fileTheEnvironmentIncludesIsNamedFromTheModuleFolder() throws IOException {
        workbook("Common.xlsx", rules -> row(rules.createSheet("Common"), 0, "Method Integer one()")
                .getSheet().createRow(1).createCell(0).setCellValue("return 1;"));
        var workbook = workbook("Main.xlsx", rules -> {
            var sheet = rules.createSheet("Main");
            row(sheet, 0, "Environment");
            row(sheet, 1, "include", "Common.xlsx");
            row(sheet, 3, "Method Integer two()");
            row(sheet, 4, "return one() + 1;");
        });
        var module = dir.resolve("Main");

        MarkdownModuleWriter.write(workbookNode(workbook), module);

        var factory = new RulesEngineFactory<>(module.toString());
        assertFalse(factory.getCompiledOpenClass().hasErrors(), () -> factory.getCompiledOpenClass().getAllMessages()
                .toString());
        assertEquals(2, TestUtils.<Integer>invoke(factory.newEngineInstance(), "two"));
    }

    @Test
    void valuesOfSpreadsheetAreWrittenAsTextAndReadBackOfTheTypesTheirCellsGaveThem() throws IOException {
        var started = Date.from(LocalDateTime.of(2011, Month.JANUARY, 1, 10, 30, 15)
                .atZone(ZoneId.systemDefault())
                .toInstant());
        var workbook = workbook("Terms.xlsx", rules -> {
            var sheet = rules.createSheet("Terms");
            row(sheet, 0, "Spreadsheet SpreadsheetResult terms()");
            row(sheet, 1, "Step", "Value", "Next");
            row(sheet, 2, "Count", 5.0, -6.0);
            row(sheet, 3, "Flag", true, false);
            row(sheet, 4, "Start", date(2010, 5, 1), started);
            row(sheet, 5, "Rate", 0.5, "high");
            row(sheet, 6, "Total", "=$Value$Count + 1");
            row(sheet, 7, "Single", 7.0);
            row(sheet, 8, "Limit : Double", 7.0, 8.0);
            row(sheet, 10, "Spreadsheet SpreadsheetResult wait()");
            row(sheet, 11, "Step", "Description", "Formula");
            row(sheet, 12, "Delay", null, 60.0);
            row(sheet, 13, "Due", "The day after", "=$Formula$Delay + 1");
        });
        var module = dir.resolve("md/Terms");

        MarkdownModuleWriter.write(workbookNode(workbook), module);

        // A file names no type: a whole number, a boolean and a date read back as the values a workbook holds
        assertEquals("""
                | Step | Value | Next |
                |---|---|---|
                | Count | 5 | -6 |
                | Flag | true | false |
                | Start | 2010-05-01 | 2011-01-01T10:30:15 |
                | Rate | 0.5 | high |
                | Total | =$Value$Count + 1 |  |
                | Single | 7 |  |
                | Limit : Double | 7 | 8 |
                """, bodyOf(module.resolve("terms.md")));
        assertEquals("""
                | Step | Description | Formula |
                |---|---|---|
                | Delay |  | 60 |
                | Due | The day after | =$Formula$Delay + 1 |
                """, bodyOf(module.resolve("wait.md")));
        var factory = new RulesEngineFactory<>(module.toString());
        var compiled = factory.getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        SpreadsheetResult terms = TestUtils.invoke(factory.newEngineInstance(), "terms");
        assertEquals(List.of(5, -6, true, false, date(2010, 5, 1), started, 0.5, "high", 6, 7, 7.0, 8.0),
                Arrays.asList(terms.getValue(0, 0), terms.getValue(0, 1), terms.getValue(1, 0), terms.getValue(1, 1),
                        terms.getValue(2, 0), terms.getValue(2, 1), terms.getValue(3, 0), terms.getValue(3, 1),
                        terms.getValue(4, 0), terms.getValue(5, 0), terms.getValue(6, 0), terms.getValue(6, 1)));
        assertEquals(resultFields(workbook, "terms"), resultFields(module, "terms"));
        // The empty explanation beside the value of a step gives the result no field
        var wait = resultFields(module, "wait");
        assertEquals(resultFields(workbook, "wait"), wait);
        assertEquals("java.lang.Integer", wait.get("$Formula$Delay"));
        assertFalse(wait.containsKey("$Description$Delay"), wait::toString);
    }

    @Test
    void tableNamedAsAnotherIsNumberedAndSheetNamedAsTheFolderIsItsRoot() throws IOException {
        var folder = Files.createDirectories(dir.resolve("Rates"));
        Files.writeString(folder.resolve("rate.md"), "");

        assertEquals(folder.resolve("rate1.md"), MarkdownModuleWriter.fileOf(folder, "Rates", "rate"));
        assertEquals(folder.resolve("Environment1.md"), MarkdownModuleWriter.fileOf(folder, null, "Environment"));
        assertEquals(folder.resolve("Young/rate.md"), MarkdownModuleWriter.fileOf(folder, "Young", "rate"));
        assertEquals(folder.resolve("Young/rate.md"), MarkdownModuleWriter.fileOf(folder, " Young.", "rate"));
        assertEquals(folder.resolve("Rates..2019/rate.md"), MarkdownModuleWriter.fileOf(folder, "Rates..2019", "rate"));
        assertThrows(IllegalArgumentException.class, () -> MarkdownModuleWriter.fileOf(folder, "../Up", "rate"));
    }

    @Test
    void tableTheCompilerRejectsIsNamedFromItsHeaderAfterTheTableItRepeats() throws IOException {
        var workbook = workbook("Totals.xlsx", rules -> {
            var totals = rules.createSheet("Totals");
            row(totals, 0, "Method Integer total(String code)");
            row(totals, 1, "return 1;");
            // The same rule again, which the compiler rejects
            row(totals, 3, "Method Integer total(String code)");
            row(totals, 4, "return 2;");
            row(totals, 6, "Test total");
            row(totals, 7, "code", "_res_");
            row(totals, 8, "Code", "Total");
            row(totals, 9, "A", 1.0);
            // A test of a rule the module does not have, with a word after its name the compiler does not read
            row(totals, 11, "Test missing missingCases (code)");
            row(totals, 12, "_res_");
            row(totals, 13, "Result");
            row(totals, 14, 1.0);
            // A part the compiler cannot read
            row(totals, 16, "TablePart Shares row one of 2");
            row(totals, 17, "Data Integer shares");
        });
        var module = dir.resolve("md/Sums");

        var written = MarkdownModuleWriter.write(workbookNode(workbook), module);

        assertEquals(List.of("Totals/total.md", "Totals/total1.md", "Totals/totalTest.md", "Totals/missingCases.md",
                "Totals/Shares.md"),
                written.files().stream().map(file -> module.relativize(file).toString().replace('\\', '/')).toList());
        var messages = new RulesEngineFactory<>(module.toString()).getCompiledOpenClass().getAllMessages();
        var duplicates = messages.stream()
                .filter(message -> message.getSummary().contains("total("))
                .map(OpenLMessage::getSourceLocation)
                .toList();
        // The folder rejects the table the workbook rejected
        assertEquals(1, duplicates.size(), messages::toString);
        assertTrue(duplicates.getFirst().contains("/total1.md?"), duplicates::toString);
    }

    @Test
    void sheetIsWrittenAsTheFolderOfANameOpenLStudioCanHold() throws IOException {
        // A sheet name may hold what a name of OpenL Studio may not: a space first, %, ;, <, >, a dot last
        var sheet = " Rates 50%; <new>.";
        var workbook = workbook("Rates.xlsx", rules -> {
            var rates = rules.createSheet(sheet);
            row(rates, 0, "SmartRules Double rate(Integer age)");
            row(rates, 1, "Age", "Rate");
            row(rates, 2, "0-17", 0.1);
            row(rates, 3, "18-120", 0.3);
        });
        var module = dir.resolve("md/Rates");

        var written = MarkdownModuleWriter.write(workbookNode(workbook), module);

        assertEquals("Rates 50__ _new_", MarkdownModuleWriter.folderNameOf(sheet));
        assertEquals(List.of(module.resolve("Rates 50__ _new_/rate.md")), written.files());
        var factory = new RulesEngineFactory<>(module.toString());
        assertFalse(factory.getCompiledOpenClass().hasErrors(), () -> factory.getCompiledOpenClass().getAllMessages()
                .toString());
        assertEquals(0.3, TestUtils.<Double>invoke(factory.newEngineInstance(), "rate", 40));
    }

    private Path workbook(String name, WorkbookContent content) throws IOException {
        var file = dir.resolve(name);
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            content.fill(workbook);
            workbook.write(out);
        }
        return file;
    }

    /** The workbook a module was compiled from, as the compiler read it. */
    private static WorkbookSyntaxNode workbookNode(Path workbook) {
        var compiled = new RulesEngineFactory<>(workbook.toString()).getCompiledOpenClass();
        var errors = compiled.getAllMessages().stream().filter(message -> message.getSeverity() == Severity.ERROR)
                .map(OpenLMessage::getSummary)
                .toList();
        // A table left out may not compile; the others do
        assertFalse(errors.stream().anyMatch(error -> error.contains("rate") || error.contains("Driver")),
                errors::toString);
        // A module holds the workbooks it includes as well
        return Arrays.stream(((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                        .getWorkbookSyntaxNodes())
                .filter(node -> node.getWorkbookSourceCodeModule().getUri().endsWith(workbook.getFileName().toString()))
                .findFirst()
                .orElseThrow();
    }

    /** The fields of the result of a Spreadsheet table of a module and their types, as a service gives them. */
    private static Map<String, String> resultFields(Path module, String table) {
        var compiled = new RulesEngineFactory<>(module.toString()).getCompiledOpenClass();
        return compiled.getOpenClassWithErrors().getTypes().stream()
                .filter(type -> type.getName().equals("SpreadsheetResult" + table))
                .map(CustomSpreadsheetResultOpenClass.class::cast)
                .findFirst()
                .orElseThrow()
                .getFields().stream()
                // An empty cell is of no type and gives the service no field
                .filter(field -> field.getType().getInstanceClass() != null)
                .collect(Collectors.toMap(IOpenField::getName, field -> field.getType().getName()));
    }

    /** The table of a file without its front matter. */
    private static String bodyOf(Path file) throws IOException {
        var text = Files.readString(file);
        return text.substring(text.indexOf("\n|") + 1);
    }

    private static Row row(Sheet sheet, int index, Object... values) {
        var row = sheet.createRow(index);
        for (var column = 0; column < values.length; column++) {
            switch (values[column]) {
                case null -> {
                    // A blank cell is no cell of the row
                }
                case Double number -> row.createCell(column).setCellValue(number);
                case Boolean flag -> row.createCell(column).setCellValue(flag);
                case Date date -> {
                    var cell = row.createCell(column);
                    cell.setCellValue(date);
                    var style = sheet.getWorkbook().createCellStyle();
                    style.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-mm-dd"));
                    cell.setCellStyle(style);
                }
                default -> row.createCell(column).setCellValue(values[column].toString());
            }
        }
        return row;
    }

    private static Date date(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @FunctionalInterface
    private interface WorkbookContent {
        void fill(XSSFWorkbook workbook);
    }
}
