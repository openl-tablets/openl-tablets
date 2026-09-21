package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.ArgumentView;
import org.openl.studio.projects.model.tables.DataHeaderView;
import org.openl.studio.projects.model.tables.DataRowView;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.studio.projects.model.tables.DatatypeAppend;
import org.openl.studio.projects.model.tables.DatatypeFieldView;
import org.openl.studio.projects.model.tables.DatatypeView;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetView;
import org.openl.studio.projects.model.tables.SmartRulesHeaderView;
import org.openl.studio.projects.model.tables.SmartRulesView;
import org.openl.studio.projects.model.tables.SpreadsheetAppend;
import org.openl.studio.projects.model.tables.SpreadsheetCellView;
import org.openl.studio.projects.model.tables.SpreadsheetRowView;
import org.openl.studio.projects.model.tables.SpreadsheetStepView;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * A table written as a header alone, the way an author leaves it while writing one, is completed by writing its
 * shape over it.
 *
 * <p>The engine binds no body view for such a table. The writers take the rows under the header instead — an
 * empty row given to a header alone — so the header stays where it is and the body is written under it, rather
 * than the header row being taken for the body and written over.
 */
class HeaderOnlyTableWriteTest {

    @TempDir
    Path tempDir;

    @Test
    void completesASpreadsheetBegunAsAHeader() throws Exception {
        var project = headerOnly("spreadsheet", "Spreadsheet ");

        new SimpleSpreadsheetTableWriter(TableTestProjects.onlyTable(project)).write(SimpleSpreadsheetView.builder()
                .name("Total")
                .returnType("Double")
                .args(List.of(argument("Double", "a"), argument("Double", "b")))
                .steps(List.of(step("Sum", "= a + b"), step("RETURN", "= $Sum")))
                .build());

        assertEquals(List.of(
                row("Spreadsheet Double Total(Double a, Double b)", null),
                row("Steps", "Value"),
                row("Sum", "= a + b"),
                row("RETURN", "= $Sum")), rawSource(project));
    }

    @Test
    void completesADatatypeBegunAsAHeader() throws Exception {
        var project = headerOnly("datatype", "Datatype Plan");

        new DatatypeTableWriter(TableTestProjects.onlyTable(project)).write(DatatypeView.builder()
                .name("Plan")
                .fields(List.of(field("String", "name"), field("Double", "premium")))
                .build());

        assertEquals(List.of(
                row("Datatype Plan", null),
                row("String", "name"),
                row("Double", "premium")), rawSource(project));
    }

    @Test
    void completesARulesTableBegunAsAHeader() throws Exception {
        var project = headerOnly("rules", "SmartRules String Price(String make)");
        var rule = new LinkedHashMap<String, Object>();
        rule.put("make", "Audi");
        rule.put("Price", "100");

        new SmartRulesWriter(TableTestProjects.onlyTable(project)).write(SmartRulesView.builder()
                .name("Price")
                .returnType("String")
                .args(List.of(argument("String", "make")))
                .headers(List.of(SmartRulesHeaderView.builder().title("make").build(),
                        SmartRulesHeaderView.builder().title("Price").build()))
                .rules(List.of(rule))
                .build());

        assertEquals(List.of(
                row("SmartRules String Price(String make)", null),
                row("make", "Price"),
                row("Audi", "100")), rawSource(project));
    }

    @Test
    void appendsFieldsRightUnderTheHeaderOfADatatypeBegunAsAHeader() throws Exception {
        var project = headerOnly("appended", "Datatype Plan");
        var appended = new DatatypeAppend();
        appended.setFields(List.of(field("String", "name"), field("Double", "premium")));

        new DatatypeTableWriter(TableTestProjects.onlyTable(project)).append(appended);

        // The first field stands right under the header: a blank row there would end the table before it.
        assertEquals(List.of(
                row("Datatype Plan", null),
                row("String", "name"),
                row("Double", "premium")), rawSource(project));
    }

    @Test
    void leavesTheTableAsItWasWhenAnAppendIsRefused() throws Exception {
        var project = headerOnly("refused", "Spreadsheet ");
        var appended = new SpreadsheetAppend();
        appended.setRows(List.of(SpreadsheetRowView.builder().name("Sum").build()));
        appended.setCells(new SpreadsheetCellView[][]{{SpreadsheetCellView.builder().value("= 1").build()}});

        // A header alone has no data columns to append a value to.
        assertThrows(BadRequestException.class,
                () -> new SpreadsheetTableWriter(TableTestProjects.onlyTable(project)).append(appended));

        // Asking for the body to check the request against did not touch the table.
        assertEquals(List.of(row("Spreadsheet")), rawSource(project));
    }

    @Test
    void writesTheBodyUnderThePropertiesOfATableBegunAsAHeaderAndProperties() throws Exception {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("description", "Adds up");
        properties.put("active", "true");
        var project = headerAndProperties("properties", "Spreadsheet Double Total()", properties);

        new SimpleSpreadsheetTableWriter(TableTestProjects.onlyTable(project)).write(SimpleSpreadsheetView.builder()
                .name("Total")
                .returnType("Double")
                .properties(properties)
                .steps(List.of(step("RETURN", "1")))
                .build());

        // The properties section is two rows deep; the body starts under it, not inside it.
        assertEquals(List.of(
                row("Spreadsheet Double Total()", null, null),
                row("properties", "description", "Adds up"),
                row(null, "active", "true"),
                row("Steps", "Value", null),
                row("RETURN", "1", null)), rawSource(project));
    }

    @Test
    void completesADataTableBegunAsAHeader() throws Exception {
        var project = headerOnly("data", "Data String names");

        new DataTableWriter(TableTestProjects.onlyTable(project)).write(DataView.builder()
                .name("names")
                .dataType("String")
                .headers(List.of(DataHeaderView.builder().fieldName("this").displayName("Name").build()))
                .rows(List.of(DataRowView.builder().values(List.of("Audi")).build()))
                .build());

        assertEquals(List.of(
                row("Data String names"),
                row("this"),
                row("Name"),
                row("Audi")), rawSource(project));
    }

    /** A project whose one module holds one table: the given header, and nothing under it. */
    private Path headerOnly(String name, String header) throws Exception {
        return TableTestProjects.writeProject(tempDir.resolve(name), name, "Rules", new String[][]{{header}});
    }

    /**
     * A project whose one table is the given header and a properties section, and nothing under them.
     *
     * <p>Written the way Excel keeps a properties section: the {@code properties} cell merged down the rows of
     * the section, one property a row.
     */
    private Path headerAndProperties(String name, String header, Map<String, Object> properties) throws Exception {
        var dir = Files.createDirectories(tempDir.resolve(name));
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Rules");
            sheet.createRow(1).createCell(1).setCellValue(header);
            var rowIndex = 2;
            for (var property : properties.entrySet()) {
                var row = sheet.createRow(rowIndex++);
                row.createCell(2).setCellValue(property.getKey());
                row.createCell(3).setCellValue(String.valueOf(property.getValue()));
            }
            sheet.getRow(2).createCell(1).setCellValue("properties");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 3));
            sheet.addMergedRegion(new CellRangeAddress(2, rowIndex - 1, 1, 1));
            try (var out = Files.newOutputStream(dir.resolve(name + ".xlsx"))) {
                workbook.write(out);
            }
        }
        return dir;
    }

    /** The table's cells as plain text, read back from the workbook the writer saved. */
    private static List<List<String>> rawSource(Path project) {
        return TableTestProjects.rawSource(TableTestProjects.onlyTable(project));
    }

    private static List<String> row(String... cells) {
        return Arrays.asList(cells);
    }

    private static ArgumentView argument(String type, String name) {
        return ArgumentView.builder().type(type).name(name).build();
    }

    private static SpreadsheetStepView step(String name, String value) {
        return SpreadsheetStepView.builder().name(name).value(value).build();
    }

    private static DatatypeFieldView field(String type, String name) {
        return DatatypeFieldView.builder().type(type).name(name).build();
    }
}
