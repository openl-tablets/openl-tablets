package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.maven.migration.RulesProjects.compile;
import static org.openl.rules.maven.migration.RulesProjects.context;
import static org.openl.rules.maven.migration.RulesProjects.engine;
import static org.openl.rules.maven.migration.RulesProjects.errors;
import static org.openl.rules.maven.migration.RulesProjects.read;
import static org.openl.rules.maven.migration.RulesProjects.row;
import static org.openl.rules.maven.migration.RulesProjects.writeProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RulesRemovedFunctionsMigratorTest {

    @Test
    void rewritesRemovedFunctionsByArgumentTypes(@TempDir Path folder) throws Exception {
        var workbook = writeProject(folder, List.of(
                List.of(row("Method String days(Integer n)"), row("return format(\"{0} days\", n);")),
                List.of(row("Method String isoDay(String text)"),
                        row("Date d = stringToDate(text);"),
                        row("return dateToString(d, \"yyyy-MM-dd\");")),
                List.of(row("Method String shortDate(String text)"),
                        row("return format(stringToDate(text), \"M/d/yy\");")),
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
                "Method String days(Integer n)",
                "return format(\"{0} days\", n);",
                "",
                "Method String isoDay(String text)",
                "Date d = toDate(text);",
                "return toString(d, \"yyyy-MM-dd\");",
                "",
                "Method String shortDate(String text)",
                "return format(toDate(text), \"M/d/yy\");",
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
                "Text|= toString(date, \"M/d/yy\") + \" / \" + format(date, \"yyyy\")"), read(workbook));
        assertEquals(List.of(), errors(compile(folder)));

        var factory = engine(folder);
        var rules = factory.newInstance();
        var type = factory.getInterfaceClass();
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
                List.of(row("Method String day(Date date)"), row("return dateToString(date);"))));
        assertTrue(inlineStrings(workbook));
        var compilations = new AtomicInteger();

        var changed = new RulesRemovedFunctionsMigrator().migrate(folder, new MigrationContext(null, new Compilation(() -> {
            compilations.incrementAndGet();
            return compile(folder);
        }), null, Set.of()));

        assertEquals(List.of(workbook.toRealPath()), changed);
        assertEquals(List.of("Method String day(Date date)", "return toString(date, \"M/d/yy\");"), read(workbook));
        assertEquals(2, compilations.get());
        assertEquals(List.of(), errors(compile(folder)));
    }

    @Test
    void leavesProjectThatCompiles(@TempDir Path folder) throws Exception {
        var workbook = writeProject(folder, List.of(
                List.of(row("Method String days(Integer n)"), row("return format(\"{0} days\", n);")),
                List.of(row("Method String amount(Double amount)"), row("return format(amount);")),
                List.of(row("Method String amountOf(double amount)"), row("return format(amount, \"#,##0\");")),
                List.of(row("Method String day(Date date)"), row("return format(date);")),
                List.of(row("Method String year(Date date)"), row("return format(date, \"yyyy\");"))));
        var bytes = Files.readAllBytes(workbook);

        var changed = new RulesRemovedFunctionsMigrator().migrate(folder, context(folder));

        assertEquals(List.of(), changed);
        assertArrayEquals(bytes, Files.readAllBytes(workbook));
        var factory = engine(folder);
        var rules = factory.newInstance();
        var type = factory.getInterfaceClass();
        var date = new GregorianCalendar(2015, Calendar.APRIL, 30).getTime();
        assertEquals("2 days", type.getMethod("days", Integer.class).invoke(rules, 2));
        assertEquals("1,234.50", type.getMethod("amount", Double.class).invoke(rules, 1234.5));
        assertEquals("1,235", type.getMethod("amountOf", double.class).invoke(rules, 1234.6));
        assertEquals("04/30/2015", type.getMethod("day", Date.class).invoke(rules, date));
        assertEquals("2015", type.getMethod("year", Date.class).invoke(rules, date));
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

    /**
     * Tells whether the first sheet keeps the texts of its cells inside the cells instead of in shared strings.
     */
    private static boolean inlineStrings(Path file) throws IOException {
        try (var in = Files.newInputStream(file); var workbook = new XSSFWorkbook(in)) {
            return workbook.getSheetAt(0).getRow(0).getCell(0).getCTCell().isSetIs();
        }
    }
}
