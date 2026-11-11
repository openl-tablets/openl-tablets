package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.tables.write.RawTableWriter;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;

/**
 * Covers a table of every kind OpenL Studio formats, created through the API with no regions: its save binds it alone
 * against the module it is written to, or lays out a Datatype and a Vocabulary from its text, and formats it as a
 * compile of the module and a Reformat of the table do.
 *
 * <p>The module holds what the new tables use, and every table compiles with no error or warning once created: the
 * look a table with errors takes is not what these cases tell. A cell {@code <} is covered by the cell on its left.
 */
class FormatOnCreateBoundTest {

    private static final String SHEET = "Rules";
    /** Where the tables the new tables use stand, out of the way of the new table. */
    private static final int BASE_ROW = 40;
    private static final String MERGED = "<";

    /** The tables the module holds before a table is created: the types and the method of grades they use. */
    private static final String[][][] BASE = {
            {{"Datatype Car"}, {"String", "model"}, {"Integer", "year"}},
            {{"SimpleRules String TestedGrade(Integer score, String level)"}, {"Score", "Level", "Grade"},
                    {">= 0", "", "G"}},
            {{"Datatype Result"}, {"String", "level"}, {"String", "grade"}},
            {{"Datatype Claim"}, {"String", "cause"}, {"Boolean", "catastrophe"}, {"Boolean", "firstParty"},
                    {"Double", "amount"}}};

    /** A Rules table written turned: its conditions and returns down its first column, its rules across. */
    private static final String[][] TURNED_RULES = {
            {"Rules String Turned(Integer score)"},
            {"C1", "score >= min", "Integer min", "Min", "1", "2"},
            {"RET1", "r", "String r", "Grade", "A", "B"}};
    /** A SmartRules table matching its conditions to the fields of its one argument. */
    private static final String[][] ARGUMENT_FIELDS = {
            {"SmartRules String ClaimCode(Claim claim)"},
            {"Cause of Loss", "Catastrophe", "First Party", "Estimated Loss Amount", "Rule Code"},
            {"COMPREHENSIVE", "true", "true", ">0", "COMP1"},
            {"COMPREHENSIVE", "false", "false", "[0;5000)", "COMP5"},
            {"COLLISION", "", "true", "", "COLL1"}};
    private static final String[][] TURNED_SIMPLE_RULES = turned(new String[][]{
            {"SimpleRules String SimpleGrade(Integer score, String level)"}, {"Score", "Level", "Grade"},
            {">= 1", "high", "A"}, {">= 2", "low", "B"}});
    private static final String[][] TURNED_SMART_RULES = turned(new String[][]{
            {"SmartRules String SmartGrade(Integer score, String level)"}, {"Score", "Level", "Grade"},
            {">= 1", "high", "A"}, {">= 2", "low", "B"}});
    private static final String[][] TURNED_SIMPLE_LOOKUP = turned(new String[][]{
            {"SimpleLookup Double SimpleRate(String region, Integer age)"}, {"Region", "18", "30"},
            {"North", "1", "2"}, {"South", "3", "4"}});
    private static final String[][] TURNED_SMART_LOOKUP = turned(new String[][]{
            {"SmartLookup Double SmartRate(String region, String zone, Integer age)"},
            {"Region", "Zone", "18", "30"}, {"North", "Z1", "1", "2"}, {"South", "Z2", "3", "4"}});

    static Stream<Arguments> tables() {
        return Stream.of(
                table("a SpreadsheetResult marking a step", new String[][]{
                        {"Spreadsheet SpreadsheetResult Quote(Double amount)"}, {"Step", "Value"},
                        {"Base", "= amount"}, {"Tax*", "= $Value$Base / 10"}, {"Note", "= \"ok\""}}),
                table("a SpreadsheetResult whose steps run across", new String[][]{
                        {"Spreadsheet SpreadsheetResult Across(Double amount)"}, {"Step", "Base", "Tax*", "Note"},
                        {"Value", "= amount", "= $Base$Value / 10", "= \"ok\""}}),
                table("a Spreadsheet returning a step", new String[][]{
                        {"Spreadsheet Double Total(Double amount)"}, {"Step", "Value"}, {"Base", "= amount"},
                        {"RETURN", "= $Value$Base * 2"}, {"Rest", "= 1"}}),
                table("a Rules table", new String[][]{
                        {"Rules String Grade(Integer score, String level)"}, {"C1", "C2", "RET1"},
                        {"score >= min", "level == l", "r"}, {"Integer min", "String l", "String r"},
                        {"Min", "Level", "Grade"}, {"1", "high", "A"}, {"2", "low", "B"}}),
                table("a Rules table written turned", TURNED_RULES),
                table("a SimpleRules table", new String[][]{
                        {"SimpleRules String SimpleGrade(Integer score, String level)"}, {"Score", "Level", "Grade"},
                        {">= 1", "high", "A"}, {">= 2", "low", "B"}}),
                table("a SmartRules table", new String[][]{
                        {"SmartRules String SmartGrade(Integer score, String level)"}, {"Score", "Level", "Grade"},
                        {">= 1", "high", "A"}, {">= 2", "low", "B"}}),
                table("a SmartRules table matching the fields of its one argument", ARGUMENT_FIELDS),
                table("a SmartRules table returning a type of two fields", new String[][]{
                        {"SmartRules Result SmartPair(Integer score)"}, {"Score", "Level", "Grade"},
                        {">= 1", "high", "A"}, {">= 2", "low", "B"}}),
                table("a SimpleRules table returning a type of two fields", new String[][]{
                        {"SimpleRules Result SimplePair(Integer score)"}, {"Score", "Level", "Grade"},
                        {">= 1", "high", "A"}, {">= 2", "low", "B"}}),
                table("a SimpleRules table written turned", TURNED_SIMPLE_RULES),
                table("a SmartRules table written turned", TURNED_SMART_RULES),
                table("a SimpleLookup table written turned", TURNED_SIMPLE_LOOKUP),
                table("a SmartLookup table written turned", TURNED_SMART_LOOKUP),
                table("a SimpleLookup table", new String[][]{
                        {"SimpleLookup Double SimpleRate(String region, Integer age)"}, {"Region", "18", "30"},
                        {"North", "1", "2"}, {"South", "3", "4"}}),
                table("a SmartLookup table", new String[][]{
                        {"SmartLookup Double SmartRate(String region, String zone, Integer age)"},
                        {"Region", "Zone", "18", "30"}, {"North", "Z1", "1", "2"}, {"South", "Z2", "3", "4"}}),
                table("a TBasic table", new String[][]{
                        {"TBasic Integer Calc(Integer n)"}, {"Operation", "Condition", "Action"},
                        {"Operation", "Condition", "Action"}, {"VAR", "x", "1"}, {"SET", "", "x += n"},
                        {"RETURN", "x", ""}}),
                table("a ColumnMatch table", new String[][]{
                        {"ColumnMatch <MATCH> String Approval(String area, Integer money)"},
                        {"names", "operation", "values", MERGED}, {"Expenses", "Condition", "Need Approval Of", MERGED},
                        {"Return Values", "", "CAO", "Team Lead"}, {"area", "match", "Hardware", "Hardware"},
                        {"money", "min", "100000", "5000"}}),
                table("a Method table", new String[][]{{"Method Double Twice(Double a)"}, {"return a * 2;"}}),
                table("a Data table", new String[][]{
                        {"Data Car cars"}, {"model", "year"}, {"Model", "Year"}, {"A4", "2020"}, {"Q7", "2021"}}),
                table("a Data table written turned", new String[][]{
                        {"Data Car carsTurned"}, {"model", "Model", "A4", "Q7"}, {"year", "Year", "2020", "2021"}}),
                table("a Test table", new String[][]{
                        {"Test TestedGrade GradeTest"}, {"score", "level", "_res_"}, {"Score", "Level", "Grade"},
                        {"1", "high", "G"}, {"2", "low", "G"}}),
                table("a Test table written turned", turned(new String[][]{
                        {"Test TestedGrade GradeTest"}, {"score", "level", "_res_"}, {"Score", "Level", "Grade"},
                        {"1", "high", "G"}, {"2", "low", "G"}})),
                table("a Run table", new String[][]{
                        {"Run TestedGrade GradeRun"}, {"score", "level"}, {"Score", "Level"}, {"1", "high"}}),
                table("a Run table written turned", turned(new String[][]{
                        {"Run TestedGrade GradeRun"}, {"score", "level"}, {"Score", "Level"}, {"1", "high"}})),
                table("a Datatype table", new String[][]{
                        {"Datatype Bike"}, {"String", "model"}, {"Integer", "year", "2020"}}),
                table("a Vocabulary table", new String[][]{{"Datatype Color <String>"}, {"Red"}, {"Green"}}),
                table("a Constants table", new String[][]{
                        {"Constants"}, {"Double", "rate", "1.5"}, {"String", "label", "Base"}}),
                table("a Properties table", new String[][]{{"Properties Defaults"}, {"scope", "Module"},
                        {"lob", "Auto"}}));
    }

    /**
     * The decision tables whose text alone tells other roles than their compile: a table written turned, which its
     * text tells upright, and a SmartRules table matching several conditions to the fields of one argument, which its
     * text gives a condition for each argument.
     */
    static Stream<Arguments> tablesTheTextTellsWrong() {
        return Stream.of(
                table("a Rules table written turned", TURNED_RULES),
                table("a SimpleRules table written turned", TURNED_SIMPLE_RULES),
                table("a SmartRules table written turned", TURNED_SMART_RULES),
                table("a SimpleLookup table written turned", TURNED_SIMPLE_LOOKUP),
                table("a SmartLookup table written turned", TURNED_SMART_LOOKUP),
                table("a SmartRules table matching the fields of its one argument", ARGUMENT_FIELDS));
    }

    private static Arguments table(String name, String[][] rows) {
        return Arguments.of(Named.of(name, rows));
    }

    /** A table written turned: its header on top, and a row of its body for each column of the table as written. */
    private static String[][] turned(String[][] rows) {
        var width = Stream.of(rows).skip(1).mapToInt(row -> row.length).max().orElse(0);
        var turned = new String[width + 1][];
        turned[0] = rows[0];
        for (var column = 0; column < width; column++) {
            var line = new String[rows.length - 1];
            for (var row = 1; row < rows.length; row++) {
                line[row - 1] = column < rows[row].length ? rows[row][column] : "";
            }
            turned[column + 1] = line;
        }
        return turned;
    }

    @ParameterizedTest
    @MethodSource("tables")
    void formatsANewTableAsItsCompileReadsIt(String[][] rows, @TempDir Path created, @TempDir Path compiled)
            throws IOException {
        create(created, rows, true);

        assertEquals(List.of(), errorsOf(TableTestProjects.projectModel(created)), "The new table compiles");
        assertEquals(compiledLooksOf(compiled, rows, cornerOf(created, rows)), looks(created));
    }

    @ParameterizedTest
    @MethodSource("tablesTheTextTellsWrong")
    void formatsATableItsTextAloneTellsWrongOnlyWithNoBind(String[][] rows, @TempDir Path guessed,
                                                           @TempDir Path compiled) throws IOException {
        create(guessed, rows, false);

        assertNotEquals(compiledLooksOf(compiled, rows, cornerOf(guessed, rows)), looks(guessed),
                "The text of the table tells other roles than its compile");
    }

    /** Creates a table in a module compiled before, bound against the module or laid out from its text alone. */
    private static void create(Path dir, String[][] rows, boolean bind) throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, FormatOnCreateBoundTest::base);
        assertEquals(List.of(), errorsOf(model), "The module compiles before the table is created");
        var formatting = mock(TableFormatting.class);
        when(formatting.onSave()).thenReturn(true);
        var properties = mock(SystemPropertiesService.class);
        when(properties.onCreate()).thenReturn(Map.of());
        var factory = new TableWritersFactory(formatting);
        var creator = new TableCreatorService(factory, new TableWriterExecutor(), mock(ProjectFilesService.class),
                mock(ProjectFileRootFactory.class), properties, formatting, null);
        var view = view(rows);
        if (bind) {
            creator.createTable(new CreateNewTableRequest("Main", SHEET, null, view, null), model);
        } else {
            var writer = factory.getNewTableWriter(view, creator.sheetGridModel(model, SHEET));
            writer.setModule(model.getXlsModuleNode());
            new TableWriterExecutor().executeWrite(writer, view);
        }
    }

    /** The module before a table is created: a note at the corner and the tables the new tables use. */
    private static void base(Sheet sheet) {
        row(sheet, 0, 0, "Notes");
        var top = BASE_ROW;
        for (var table : BASE) {
            write(sheet, table, top, 0);
            top += table.length + 1;
        }
    }

    /** Writes a table on a sheet, a cell {@code <} merged into the cell on its left, the header across the table. */
    private static void write(Sheet sheet, String[][] rows, int top, int left) {
        var width = 1;
        for (var line = 0; line < rows.length; line++) {
            var cells = rows[line];
            row(sheet, top + line, left, Stream.of(cells).map(cell -> MERGED.equals(cell) ? null : cell)
                    .toArray(String[]::new));
            width = line == 0 ? width : Math.max(width, cells.length);
            mergeCovered(sheet, cells, top + line, left);
        }
        if (width > 1) {
            merge(sheet, top, top, left, left + width - 1);
        }
    }

    /** Merges each cell of a line with the cells {@code <} right of it. */
    private static void mergeCovered(Sheet sheet, String[] cells, int row, int left) {
        for (var column = 0; column < cells.length; column++) {
            var span = spanOf(cells, column);
            if (span > 1) {
                merge(sheet, row, row, left + column, left + column + span - 1);
            }
        }
    }

    /** How many cells a cell of a line takes: itself and the cells {@code <} right of it. */
    private static int spanOf(String[] cells, int column) {
        if (MERGED.equals(cells[column])) {
            return 0;
        }
        var span = 1;
        while (column + span < cells.length && MERGED.equals(cells[column + span])) {
            span++;
        }
        return span;
    }

    /** The raw view of a table as an API client sends it, with no regions. */
    private static RawTableView view(String[][] rows) {
        var source = new ArrayList<List<RawTableCell>>();
        for (var cells : rows) {
            var line = new ArrayList<RawTableCell>();
            for (var column = 0; column < cells.length; column++) {
                var span = spanOf(cells, column);
                var cell = span == 0
                        ? RawTableCell.builder().covered(true)
                        : RawTableCell.builder().value(cells[column]).colspan(span > 1 ? span : null);
                line.add(cell.build());
            }
            source.add(line);
        }
        return RawTableView.builder().kind(TableKind.OTHER).name("Created").source(source).build();
    }

    /**
     * The look of the table written at the same place into a module of its own, compiled, and formatted whole in the
     * roles its compile gives it, as a Reformat formats it.
     */
    private static List<String> compiledLooksOf(Path dir, String[][] rows, int[] corner) throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            base(sheet);
            write(sheet, rows, corner[0], corner[1]);
        });
        assertEquals(List.of(), errorsOf(model), "The table compiles");
        var table = TableTestProjects.tableHeaded(model, rows[0][0]);
        var writer = new RawTableWriter(table);
        writer.formatOnSave(true);
        writer.apply(List.of(new RawTableSourceAction.Update(new UpdateTarget.Cell(0, 0, rows[0][0]))));
        return looks(dir);
    }

    /** The row and the column of the sheet the header of a table stands at. */
    private static int[] cornerOf(Path dir, String[][] rows) throws IOException {
        try (var in = Files.newInputStream(dir.resolve(SHEET + ".xlsx")); var workbook = new XSSFWorkbook(in)) {
            for (var line : workbook.getSheet(SHEET)) {
                for (var cell : line) {
                    if (rows[0][0].equals(cell.getStringCellValue())) {
                        return new int[]{cell.getRowIndex(), cell.getColumnIndex()};
                    }
                }
            }
        }
        throw new IllegalStateException("No table headed " + rows[0][0]);
    }

    private static List<String> errorsOf(ProjectModel model) {
        return model.getModuleMessages()
                .stream()
                .filter(message -> message.getSeverity() != Severity.INFO)
                .map(OpenLMessage::getSummary)
                .toList();
    }

    private static List<String> looks(Path dir) throws IOException {
        return TableTestProjects.looksOf(dir.resolve(SHEET + ".xlsx"), SHEET);
    }
}
