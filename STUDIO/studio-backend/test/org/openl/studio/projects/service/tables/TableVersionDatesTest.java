package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.ExecutableNodeView;
import org.openl.studio.projects.model.tables.TableGraphNodeKind;
import org.openl.studio.projects.service.tables.graph.GraphLayer;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;
import org.openl.studio.projects.service.tables.read.VocabularyTableReader;

/**
 * Verifies that a version of a table is told by the values of its dimension properties, not by a name the server
 * formats.
 *
 * <p>The module writes {@code carPrice} in two versions, one effective from a day and one from a time of that day.
 * Both inherit the state from the file name of the module. The table list and the dependency graph answer the same
 * values, each in its own type.
 */
class TableVersionDatesTest {

    private static final Date NEW_YEAR = dateOf(LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0));
    private static final Date HALF_PAST_TEN = dateOf(LocalDateTime.of(2020, Month.JULY, 1, 10, 30));

    private static ProjectModel model;

    @BeforeAll
    static void writeModule(@TempDir Path projectDir) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("m/d/yy h:mm"));
            var sheet = workbook.createSheet("Rules");
            carPrice(sheet, 1, NEW_YEAR, dateStyle, "Cheap");
            carPrice(sheet, 9, HALF_PAST_TEN, dateStyle, "Dear");
            // a table a reader knows by the name its property gives
            row(sheet, 17, "Method String discount()");
            row(sheet, 18, "properties", "name", "Seasonal Discount");
            row(sheet, 19, "return \"None\";");
            try (OutputStream out = Files.newOutputStream(projectDir.resolve("AR.xlsx"))) {
                workbook.write(out);
            }
        }
        // The module gives every table of it the state its file name says.
        Files.writeString(projectDir.resolve("rules.xml"), """
                <project>
                    <name>Prices</name>
                    <modules>
                        <module>
                            <name>Prices</name>
                            <rules-root path="AR.xlsx"/>
                        </module>
                    </modules>
                    <properties-file-name-pattern>%state%</properties-file-name-pattern>
                </project>
                """);
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        model = new ProjectModel(mock(WebStudio.class), null);
        model.setModuleInfo(module);
    }

    @Test
    void theTableListAnswersTheDimensionsOfEveryVersion() {
        var reader = new SummaryTableReader();
        var overloads = model.getMethodNodesDictionary();
        var dimensions = model.getAllTableSyntaxNodes()
                .stream()
                .map(TableSyntaxNodeAdapter::new)
                .map(table -> reader.read(table, overloads))
                .filter(table -> "carPrice".equals(table.name))
                .map(table -> table.dimensions)
                .toList();

        assertDimensions(dimensions);
    }

    @Test
    void theGraphAnswersTheSameDimensionsUnderTheNameOfTheTable() {
        var service = new ProjectTablesGraphService(new SummaryTableReader(), new VocabularyTableReader());
        var nodes = service.buildProjectGraph(model, false, GraphLayer.ALL)
                .stream()
                .map(node -> assertInstanceOf(ExecutableNodeView.class, node))
                .toList();

        // The versions are named as the table is, the dispatcher first; what tells them apart is in their dimensions.
        // A table with a name property is named by it.
        assertEquals(List.of("carPrice", "carPrice", "carPrice", "Seasonal Discount"),
                nodes.stream().map(node -> node.name).toList());
        var dispatcher = nodes.getFirst();
        assertEquals(TableGraphNodeKind.DISPATCHER, dispatcher.kind);
        // the dispatcher selects a version, and is told by no dimension itself
        assertNull(dispatcher.dimensions);
        assertDimensions(nodes.subList(1, 3).stream().map(node -> node.dimensions).toList());
        // the versions follow it in the order of their ids, the same in every answer
        assertEquals(nodes.subList(1, 3).stream().map(node -> node.id).sorted().toList(),
                nodes.subList(1, 3).stream().map(node -> node.id).toList());
        assertNull(nodes.getLast().dimensions);
    }

    /**
     * Both versions answer the date they are effective from and the state they inherit, keyed by the technical names
     * in the order of the property definitions.
     */
    private static void assertDimensions(List<Map<String, Object>> dimensions) {
        assertEquals(2, dimensions.size());
        dimensions.forEach(version -> {
            assertEquals(List.of("effectiveDate", "state"), List.copyOf(version.keySet()));
            assertArrayEquals(new UsStatesEnum[]{UsStatesEnum.AR}, (Object[]) version.get("state"));
        });
        assertEquals(List.of(NEW_YEAR, HALF_PAST_TEN),
                dimensions.stream().map(version -> (Date) version.get("effectiveDate")).sorted().toList());
    }

    /** Writes one version of the carPrice table, effective from the given moment. */
    private static void carPrice(Sheet sheet, int firstRow, Date effectiveDate, CellStyle dateStyle, String price) {
        row(sheet, firstRow, "Rules String carPrice(String make)");
        // the date is written as an Excel date, in the column after the name of the property
        var date = row(sheet, firstRow + 1, "properties", "effectiveDate").createCell(3);
        date.setCellValue(effectiveDate);
        date.setCellStyle(dateStyle);
        row(sheet, firstRow + 2, "C1", "RET1");
        row(sheet, firstRow + 3, "make == c1", "value");
        row(sheet, firstRow + 4, "String c1", "String value");
        row(sheet, firstRow + 5, "Make", "Price");
        row(sheet, firstRow + 6, "Toyota", price);
    }

    /** Writes the values from the second column on. */
    private static Row row(Sheet sheet, int rowIndex, String... values) {
        var row = sheet.createRow(rowIndex);
        for (var column = 0; column < values.length; column++) {
            row.createCell(column + 1).setCellValue(values[column]);
        }
        return row;
    }

    private static Date dateOf(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
