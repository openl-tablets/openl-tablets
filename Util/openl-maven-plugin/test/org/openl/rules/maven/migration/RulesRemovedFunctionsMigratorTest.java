package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;

class RulesRemovedFunctionsMigratorTest {

    private static final String RULES_XML = """
            <project>
                <name>migrate</name>
                <modules>
                    <module>
                        <name>Main</name>
                        <rules-root path="Main.xlsx"/>
                    </module>
                </modules>
            </project>
            """;

    @Test
    void rewritesRemovedFunctionsByArgumentTypes(@TempDir Path folder) throws Exception {
        var workbook = writeProject(folder, List.of(
                List.of(row("Method String amount(Double amount)"), row("return format(amount);")),
                List.of(row("Method String amountOf(double amount)"), row("return format(amount, \"#,##0\");")),
                List.of(row("Method String days(Integer n)"), row("return format(\"{0} days\", n);")),
                List.of(row("Method String isoDay(String text)"),
                        row("Date d = stringToDate(text);"),
                        row("return dateToString(d, \"yyyy-MM-dd\");")),
                List.of(row("Method String shortDate(String text)"), row("return format(stringToDate(text));")),
                List.of(row("Method String fixed(Double amount)"),
                        row("return toString(amount, DEFAULT_DOUBLE_FORMAT);")),
                List.of(row("Method Double number(String text)"), row("return parseFormattedDouble(text);")),
                List.of(row("Method Double twice(String text)"), row("return parseFormattedDouble(text) * 2;")),
                List.of(row("Method String[] addName(String[] names, String name)"),
                        row("return addIgnoreNull(names, name);")),
                List.of(row("Method String[] insertName(String[] names, String name)"),
                        row("return addArrayElementIgnoreNull(names, 0, name);")),
                List.of(row("Method String firstName(String[] names, String name)"),
                        row("return addIgnoreNull(names, name)[0];")),
                List.of(row("Spreadsheet SpreadsheetResult calc(Date date)"),
                        row("Step", "Value"),
                        row("Text", "= dateToString(date) + \" / \" + format(date, \"yyyy\")"))));

        var changed = new RulesRemovedFunctionsMigrator().migrate(folder, context(folder));

        assertEquals(List.of(workbook.toRealPath()), changed);
        assertEquals(List.of(
                "Method String amount(Double amount)",
                "return toString(amount, \"#,##0.00\");",
                "",
                "Method String amountOf(double amount)",
                "return toString(amount, \"#,##0\");",
                "",
                "Method String days(Integer n)",
                "return format(\"{0} days\", n);",
                "",
                "Method String isoDay(String text)",
                "Date d = toDate(text);",
                "return toString(d, \"yyyy-MM-dd\");",
                "",
                "Method String shortDate(String text)",
                "return toString(toDate(text), \"M/d/yy\");",
                "",
                "Method String fixed(Double amount)",
                "return toString(amount, \"#,##0.00\");",
                "",
                "Method Double number(String text)",
                "return (Double) toNumber(text);",
                "",
                "Method Double twice(String text)",
                "return ((Double) toNumber(text)) * 2;",
                "",
                "Method String[] addName(String[] names, String name)",
                "return name == null ? names : add(names, name);",
                "",
                "Method String[] insertName(String[] names, String name)",
                "return name == null ? names : addElement(names, 0, name);",
                "",
                "Method String firstName(String[] names, String name)",
                "return (name == null ? names : add(names, name))[0];",
                "",
                "Spreadsheet SpreadsheetResult calc(Date date)",
                "Step|Value",
                "Text|= toString(date, \"M/d/yy\") + \" / \" + toString(date, \"yyyy\")"), read(workbook));
        assertEquals(List.of(), errors(compile(folder)));

        var factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject(folder.toString())
                .build();
        var rules = factory.newInstance();
        var type = factory.getInterfaceClass();
        assertEquals("1,234.50", type.getMethod("amount", Double.class).invoke(rules, 1234.5));
        assertEquals("1,235", type.getMethod("amountOf", double.class).invoke(rules, 1234.6));
        assertEquals("2 days", type.getMethod("days", Integer.class).invoke(rules, 2));
        assertEquals("2015-04-30", type.getMethod("isoDay", String.class).invoke(rules, "04/30/2015"));
        assertEquals("4/30/15", type.getMethod("shortDate", String.class).invoke(rules, "04/30/2015"));
        assertEquals("0.50", type.getMethod("fixed", Double.class).invoke(rules, 0.5));
        var number = type.getMethod("number", String.class);
        assertEquals(1441.33, number.invoke(rules, "1,441.33"));
        assertEquals(12.0, number.invoke(rules, "12"));
        assertNull(number.invoke(rules, "12abc"));
        assertEquals(2882.66, type.getMethod("twice", String.class).invoke(rules, "1,441.33"));
        var names = new String[]{"a"};
        var addName = type.getMethod("addName", String[].class, String.class);
        assertArrayEquals(names, (String[]) addName.invoke(rules, names, null));
        assertArrayEquals(new String[]{"a", "b"}, (String[]) addName.invoke(rules, names, "b"));
        var insertName = type.getMethod("insertName", String[].class, String.class);
        assertArrayEquals(new String[]{"b", "a"}, (String[]) insertName.invoke(rules, names, "b"));
        assertEquals("a", type.getMethod("firstName", String[].class, String.class).invoke(rules, names, null));
    }

    @Test
    void rewritesCellStoredAsInlineString(@TempDir Path folder) throws IOException {
        var workbook = writeProject(folder, new SXSSFWorkbook(), List.of(
                List.of(row("Method String amount(Double amount)"), row("return format(amount);"))));
        assertTrue(inlineStrings(workbook));
        var compilations = new AtomicInteger();

        var changed = new RulesRemovedFunctionsMigrator().migrate(folder, new MigrationContext(null, () -> {
            compilations.incrementAndGet();
            return compile(folder);
        }));

        assertEquals(List.of(workbook.toRealPath()), changed);
        assertEquals(List.of("Method String amount(Double amount)", "return toString(amount, \"#,##0.00\");"),
                read(workbook));
        assertEquals(2, compilations.get());
        assertEquals(List.of(), errors(compile(folder)));
    }

    @Test
    void leavesProjectThatCompiles(@TempDir Path folder) throws IOException {
        var workbook = writeProject(folder, List.of(
                List.of(row("Method String days(Integer n)"), row("return format(\"{0} days\", n);"))));
        var bytes = Files.readAllBytes(workbook);

        var changed = new RulesRemovedFunctionsMigrator().migrate(folder, context(folder));

        assertEquals(List.of(), changed);
        assertArrayEquals(bytes, Files.readAllBytes(workbook));
    }

    @Test
    void leavesCallWithoutReplacement(@TempDir Path folder) throws IOException {
        var workbook = writeProject(folder, List.of(
                List.of(row("Method Double amount(String text)"),
                        row("return parseFormattedDouble(text, \"$#,##0.00\");"))));

        var changed = new RulesRemovedFunctionsMigrator().migrate(folder, context(folder));

        assertEquals(List.of(), changed);
        assertEquals(List.of("Method Double amount(String text)",
                "return parseFormattedDouble(text, \"$#,##0.00\");"), read(workbook));
        assertTrue(errors(compile(folder)).getFirst().contains("parseFormattedDouble"));
    }

    private static MigrationContext context(Path folder) {
        return new MigrationContext(null, () -> compile(folder));
    }

    private static Collection<OpenLMessage> compile(Path folder) {
        try {
            return new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                    .setProject(folder.toString())
                    .setExecutionMode(false)
                    .build()
                    .getCompiledOpenClass()
                    .getAllMessages();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> errors(Collection<OpenLMessage> messages) {
        return messages.stream().filter(m -> m.getSeverity() == Severity.ERROR).map(OpenLMessage::getSummary).toList();
    }

    private static String[] row(String... cells) {
        return cells;
    }

    /**
     * Writes the tables one under another, with an empty row between them, and merges the header of a table over
     * all its columns.
     */
    private static Path writeProject(Path folder, List<List<String[]>> tables) throws IOException {
        return writeProject(folder, new XSSFWorkbook(), tables);
    }

    /**
     * Writes the tables with the workbook, which decides how the texts of the cells are stored.
     */
    private static Path writeProject(Path folder, Workbook book, List<List<String[]>> tables) throws IOException {
        Files.writeString(folder.resolve("rules.xml"), RULES_XML);
        var file = folder.resolve("Main.xlsx");
        try (var workbook = book; var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Main");
            var r = 0;
            for (var table : tables) {
                var width = table.stream().mapToInt(cells -> cells.length).max().orElse(1);
                if (width > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(r, r, 0, width - 1));
                }
                for (var cells : table) {
                    var row = sheet.createRow(r++);
                    for (var c = 0; c < cells.length; c++) {
                        row.createCell(c).setCellValue(cells[c]);
                    }
                }
                r++;
            }
            workbook.write(out);
        }
        return file;
    }

    /**
     * Tells whether the first sheet keeps the texts of its cells inside the cells instead of in shared strings.
     */
    private static boolean inlineStrings(Path file) throws IOException {
        try (var in = Files.newInputStream(file); var workbook = new XSSFWorkbook(in)) {
            return workbook.getSheetAt(0).getRow(0).getCell(0).getCTCell().isSetIs();
        }
    }

    /**
     * Reads the rows of the first sheet, joining the texts of the cells of a row with {@code |}.
     */
    private static List<String> read(Path file) throws IOException {
        try (var workbook = WorkbookFactory.create(file.toFile(), null, true)) {
            var sheet = workbook.getSheetAt(0);
            return IntStream.rangeClosed(0, sheet.getLastRowNum())
                    .mapToObj(sheet::getRow)
                    .map(row -> row == null ? "" : StreamSupport.stream(row.spliterator(), false)
                            .map(Cell::getStringCellValue)
                            .reduce((a, b) -> a + "|" + b)
                            .orElse(""))
                    .toList();
        }
    }
}
