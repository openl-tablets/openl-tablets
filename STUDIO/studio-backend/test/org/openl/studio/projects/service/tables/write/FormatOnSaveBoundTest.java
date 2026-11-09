package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Sheet;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers an edit that changes how the compiler reads a table: the save binds the table alone and formats it in the
 * roles the edit gives, as a compile of the edited table formats it.
 *
 * <p>Every table compiles with no error or warning before the edit and after it: the look a table with errors takes
 * is not what these cases tell. An edit writes several cells at one save where one would leave the table broken.
 */
class FormatOnSaveBoundTest {

    private static final String SHEET = "Rules";
    private static final int HEADER = 1;
    private static final int LEFT = 1;
    /** Rules enough that a save binds the first rows of a table alone before it binds the whole of it. */
    private static final int RULES = 100;

    /** A cell an edit writes, in table coordinates: the header row is row 0. */
    record Write(int row, int column, String text) {
    }

    /**
     * A role-changing edit, saved at once.
     *
     * @param table  the table before the edit, at the top of the sheet, its header merged across it
     * @param others the tables the table uses, under it
     * @param writes the cells the edit writes
     */
    record Edit(String[][] table, List<String[][]> others, List<Write> writes) {

        /** The sheet before the edit. */
        Consumer<Sheet> before() {
            return sheetOf(table, others);
        }

        /** The sheet as the edit leaves it. */
        Consumer<Sheet> after() {
            var edited = Arrays.stream(table).map(String[]::clone).toArray(String[][]::new);
            writes.forEach(write -> edited[write.row()][write.column()] = write.text());
            return sheetOf(edited, others);
        }
    }

    static Stream<Arguments> edits() {
        return Stream.of(
                Arguments.of(Named.of("a SpreadsheetResult made to return a number", new Edit(
                        spreadsheet("Spreadsheet SpreadsheetResult Calc(Double a)"), List.of(),
                        List.of(new Write(0, 0, "Spreadsheet Double Calc(Double a)"))))),
                Arguments.of(Named.of("a SpreadsheetResult given a step to return", new Edit(
                        spreadsheet("Spreadsheet SpreadsheetResult Calc(Double a)"), List.of(),
                        List.of(new Write(2, 0, "Doubled*"))))),
                Arguments.of(Named.of("a Spreadsheet given the step it returns", new Edit(
                        spreadsheet("Spreadsheet Double Calc(Double a)"), List.of(),
                        List.of(new Write(3, 0, "RETURN"))))),
                Arguments.of(Named.of("a SpreadsheetResult given a step to return below the first rows", new Edit(
                        manySteps("SpreadsheetResult"), List.of(), List.of(new Write(92, 0, "Step90*"))))),
                Arguments.of(Named.of("a Spreadsheet of many steps made to return them all", new Edit(
                        manySteps("Double"), List.of(),
                        List.of(new Write(0, 0, "Spreadsheet SpreadsheetResult Steps(Double a)"))))),
                Arguments.of(Named.of("a Rules table whose condition and return change places", new Edit(
                        rules(), List.of(), List.of(
                        new Write(1, 1, "RET1"), new Write(2, 1, "l"),
                        new Write(1, 2, "C2"), new Write(2, 2, "level == r"))))),
                Arguments.of(Named.of("a SmartRules table made to return a type of two fields", new Edit(
                        grades("SmartRules String Grade(Integer score, String level)"), List.<String[][]>of(RESULT),
                        List.of(new Write(0, 0, "SmartRules Result Grade(Integer score)"))))),
                Arguments.of(Named.of("a SimpleRules table made to return a type of two fields", new Edit(
                        grades("SimpleRules String Grade(Integer score, String level)"), List.<String[][]>of(RESULT),
                        List.of(new Write(0, 0, "SimpleRules Result Grade(Integer score)"))))),
                Arguments.of(Named.of("a SimpleLookup table given one condition more", new Edit(
                        lookup("SimpleLookup Double Rate(String region, Integer age)", "Region", "18"), List.of(),
                        List.of(new Write(0, 0,
                                "SimpleLookup Double Rate(String region, String zone, Integer age)"))))),
                Arguments.of(Named.of("a SmartLookup table given one condition less", new Edit(
                        lookup("SmartLookup Double Rate(String region, String zone, Integer age)", "Region", "Zone"),
                        List.of(), List.of(new Write(0, 0, "SmartLookup Double Rate(String region, Integer age)"),
                        new Write(1, 1, "10"))))),
                Arguments.of(Named.of("a TBasic table given a step that returns", new Edit(
                        TBASIC, List.of(),
                        List.of(new Write(4, 0, "RETURN"), new Write(4, 1, "x + n"), new Write(4, 2, ""))))),
                Arguments.of(Named.of("a Test table whose argument and expected result change places", new Edit(
                        TEST, List.<String[][]>of(TESTED),
                        List.of(new Write(1, 1, "_res_"), new Write(1, 2, "level"))))),
                Arguments.of(Named.of("a Data table given keys of its own", new Edit(
                        people(), List.<String[][]>of(PERSON), List.of(new Write(1, 0, "_PK_"))))));
    }

    @ParameterizedTest
    @MethodSource("edits")
    void formatsATableInTheRolesItsEditGives(Edit edit, @TempDir Path bound, @TempDir Path shifted,
                                             @TempDir Path compiled) throws IOException {
        save(bound, edit, true);
        save(shifted, edit, false);
        // The table as the edit leaves it, compiled and formatted whole by a save that changes no value.
        var model = TableTestProjects.projectModel(compiled, SHEET, edit.after());
        assertEquals(List.of(), errorsOf(model), "The edited table compiles");
        saveEdit(mainTable(model), null, List.of(new Write(0, 0, headerOf(model))));

        var expected = looks(compiled);
        assertEquals(expected, looks(bound), "Formatted in the roles the edit gives");
        assertNotEquals(expected, looks(shifted), "The roles of the last compile are not the roles the edit gives");
    }

    private static void save(Path dir, Edit edit, boolean bind) throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, edit.before());
        assertEquals(List.of(), errorsOf(model), "The table compiles before the edit");
        var table = mainTable(model);
        var module = bind ? model.compiledModuleOf(table.getSyntaxNode()) : null;
        if (bind && module == null) {
            throw new IllegalStateException("No compiled module holds the table");
        }
        saveEdit(table, module, edit.writes());
    }

    private static void saveEdit(IOpenLTable table, @Nullable XlsModuleOpenClass module, List<Write> writes) {
        var writer = new RawTableWriter(table);
        writer.formatOnSave(true);
        writer.setCompiledModule(module);
        writer.apply(writes.stream()
                .<RawTableSourceAction>map(write -> new RawTableSourceAction.Update(
                        new UpdateTarget.Cell(write.row(), write.column(), write.text())))
                .toList());
    }

    private static List<String> errorsOf(ProjectModel model) {
        return model.getModuleMessages()
                .stream()
                .filter(message -> message.getSeverity() != Severity.INFO)
                .map(OpenLMessage::getSummary)
                .toList();
    }

    /** The table the edit is made in: the one at the top of the sheet. */
    private static IOpenLTable mainTable(ProjectModel model) {
        return model.getAllTableSyntaxNodes()
                .stream()
                .filter(node -> node.getGridTable().getRegion().getTop() == HEADER)
                .map(TableSyntaxNodeAdapter::new)
                .findFirst()
                .orElseThrow();
    }

    private static String headerOf(ProjectModel model) {
        return mainTable(model).getGridTable().getCell(0, 0).getStringValue();
    }

    private static List<String> looks(Path dir) throws IOException {
        return TableTestProjects.looksOf(dir.resolve(SHEET + ".xlsx"), SHEET);
    }

    /** A sheet holding a table at its top and the tables it uses under it, each header merged across its table. */
    private static Consumer<Sheet> sheetOf(String[][] table, List<String[][]> others) {
        return sheet -> {
            var top = HEADER;
            var tables = new ArrayList<String[][]>();
            tables.add(table);
            tables.addAll(others);
            for (var each : tables) {
                for (var line = 0; line < each.length; line++) {
                    row(sheet, top + line, LEFT, each[line]);
                }
                var width = Arrays.stream(each).skip(1).mapToInt(cells -> cells.length).max().orElse(1);
                if (width > 1) {
                    merge(sheet, top, top, LEFT, LEFT + width - 1);
                }
                top += each.length + 2;
            }
        };
    }

    private static String[][] spreadsheet(String header) {
        return new String[][]{{header}, {"Step", "Value"}, {"Doubled", "= a * 2"},
                {"Total", "= $Value$Doubled + 1"}, {"Rest", "= 1"}};
    }

    /** A Spreadsheet of more steps than a probe binds, none marked to return, returning the given type. */
    private static String[][] manySteps(String returned) {
        var lines = new ArrayList<String[]>(List.of(
                new String[]{"Spreadsheet " + returned + " Steps(Double a)"}, new String[]{"Step", "Value"}));
        IntStream.range(0, RULES).forEach(step -> lines.add(new String[]{"Step" + step, "= a + " + step}));
        return lines.toArray(String[][]::new);
    }

    /** A Rules table of a condition on the score, a condition on the level and a return. */
    private static String[][] rules() {
        var lines = new ArrayList<String[]>(List.of(
                new String[]{"Rules String Grade(Integer score, String level)"},
                new String[]{"C1", "C2", "RET1"},
                new String[]{"score >= min", "level == l", "r"},
                new String[]{"Integer min", "String l", "String r"},
                new String[]{"Min", "Level", "Grade"}));
        IntStream.range(0, RULES)
                .forEach(rule -> lines.add(
                        new String[]{String.valueOf(rule), rule % 2 == 0 ? "high" : "low", "G" + rule}));
        return lines.toArray(String[][]::new);
    }

    /** A decision table of a condition on the score, a condition on the level and the grade it returns. */
    private static String[][] grades(String header) {
        var lines = new ArrayList<String[]>(List.of(new String[]{header}, new String[]{"Score", "Level", "Grade"}));
        IntStream.range(0, RULES)
                .forEach(rule -> lines.add(new String[]{">= " + rule, rule % 2 == 0 ? "high" : "low", "G" + rule}));
        return lines.toArray(String[][]::new);
    }

    /** The type a decision table returns its level and its grade in. */
    private static final String[][] RESULT = {{"Datatype Result"}, {"String", "level"}, {"String", "grade"}};

    /** A lookup table of the regions down its first column, a second column and the ages across its first row. */
    private static String[][] lookup(String header, String first, String second) {
        var lines = new ArrayList<String[]>(List.of(new String[]{header}, new String[]{first, second, "30", "50"}));
        IntStream.range(0, RULES)
                .forEach(rule -> lines.add(new String[]{"R" + rule, String.valueOf(rule), "1", "2"}));
        return lines.toArray(String[][]::new);
    }

    /** A TBasic table that sets a number and returns it. */
    private static final String[][] TBASIC = {{"TBasic Integer Calc(Integer n)"},
            {"Operation", "Condition", "Action"}, {"Operation", "Condition", "Action"},
            {"VAR", "x", "1"}, {"SET", "", "x += n"}, {"RETURN", "x", ""}};

    /** A Test table of the method it calls. */
    private static final String[][] TEST = {{"Test TestedGrade GradeTest"}, {"score", "level", "_res_"},
            {"Score", "Level", "Grade"}, {"1", "high", "G"}, {"2", "low", "G"}};

    /** The method a Test table calls. */
    private static final String[][] TESTED = {{"SimpleRules String TestedGrade(Integer score, String level)"},
            {"Score", "Level", "Grade"}, {">= 0", "", "G"}};

    /** A Data table of people, read by the name in its first column. */
    private static String[][] people() {
        var lines = new ArrayList<String[]>(List.of(new String[]{"Data Person people"}, new String[]{"name", "age"},
                new String[]{"Name", "Age"}));
        IntStream.range(0, RULES).forEach(person -> lines.add(new String[]{"P" + person, String.valueOf(person)}));
        return lines.toArray(String[][]::new);
    }

    /** The type of the rows of a Data table of people. */
    private static final String[][] PERSON = {{"Datatype Person"}, {"String", "name"}, {"Integer", "age"}};
}
