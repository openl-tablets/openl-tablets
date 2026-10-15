package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;
import static org.openl.studio.projects.service.tables.TableTestProjects.styleNameAt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.DeleteTarget;
import org.openl.studio.projects.model.tables.InsertTarget;
import org.openl.studio.projects.model.tables.RawCellInput;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.model.tables.TableProperty;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.studio.projects.service.tables.SystemPropertiesService;
import org.openl.studio.projects.service.tables.TableCopyServiceImpl;
import org.openl.studio.projects.service.tables.TableCreatorService;
import org.openl.studio.projects.service.tables.TableFormatting;
import org.openl.studio.projects.service.tables.TablePropertiesServiceImpl;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.TableVersionService;

/**
 * Covers a table formatted with the OpenL table style when it is saved, with no compile: in the regions of its last
 * compile, moved along the lines the edit inserts and removes.
 *
 * @author Yury Molchan
 */
class FormatOnSaveTest {

    private static final String SHEET = "Rules";

    /** A SmartRules table at B2: the hour of its first two rules merged, which makes them a group. */
    private static final int HEADER = 1;
    private static final int TITLES = 2;
    private static final int FIRST_RULE = 3;
    private static final int GROUPED_RULE = 5;
    private static final int HOUR = 1;
    private static final int DAY = 2;
    private static final int RATE = 3;

    @TempDir
    Path dir;

    private ProjectModel model;

    @BeforeEach
    void writeProject() throws IOException {
        model = TableTestProjects.projectModel(dir, SHEET, FormatOnSaveTest::fillSheet);
    }

    @Test
    void formatsTheTableInTheRolesOfItsLastCompileWhenItIsSaved() throws IOException {
        save(true, update(FIRST_RULE - HEADER, DAY, "Holiday"));

        assertEquals("Title", styleAt(HEADER, HOUR));
        assertEquals("Heading 3", styleAt(TITLES, DAY));
        assertEquals("40% - Accent1", styleAt(TITLES, RATE));
        assertEquals("Input", styleAt(FIRST_RULE, DAY));
        assertEquals("Output", styleAt(GROUPED_RULE, RATE));
        assertEquals(BorderStyle.HAIR, lineAbove(GROUPED_RULE, DAY), "A line sets the group apart");
    }

    @Test
    void givesARuleInsertedUnderTheTitlesTheRolesOfARule() throws IOException {
        save(true, insertRow(FIRST_RULE - HEADER, "< 6", "Holiday", "3"));

        assertEquals("Heading 3", styleAt(TITLES, HOUR), "The titles stay where they were");
        assertEquals("Input", styleAt(FIRST_RULE, HOUR), "The new rule");
        assertEquals("Output", styleAt(FIRST_RULE, RATE));
        assertEquals(BorderStyle.HAIR, lineAbove(GROUPED_RULE + 1, DAY), "The group moved down with its rules");
        assertEquals(BorderStyle.NONE, lineAbove(GROUPED_RULE, DAY));
    }

    @Test
    void movesTheRolesUpWhenARuleIsRemoved() throws IOException {
        save(true, new RawTableSourceAction.Delete(new DeleteTarget.Rows(FIRST_RULE - HEADER, 2)));

        assertEquals("Input", styleAt(FIRST_RULE, DAY), "The rule of the second group moved up");
        assertEquals(BorderStyle.NONE, lineAbove(FIRST_RULE, DAY), "Its group is gone with the first rules");
    }

    @Test
    void leavesTheTableAsWrittenWhileFormattingOnSaveIsOff() throws IOException {
        save(false, update(FIRST_RULE - HEADER, DAY, "Holiday"));

        assertNull(styleAt(HEADER, HOUR));
        assertNull(styleAt(FIRST_RULE, DAY));
    }

    @Test
    void leavesTheTableAsWrittenOnceItsWorkbookWasWrittenSinceTheCompile() throws IOException {
        var table = table();
        save(table, false, update(FIRST_RULE - HEADER, DAY, "Holiday"));

        // What the compile read no longer answers for the sheet: the next save waits for a compile.
        save(table, true, update(FIRST_RULE - HEADER, DAY, "Feast"));

        assertNull(styleAt(FIRST_RULE, DAY));
    }

    @Test
    void formatsAPropertyWrittenUnderTheHeaderAndMovesTheBodyDown() throws IOException {
        var service = new TablePropertiesServiceImpl(mock(SystemPropertiesService.class), formattingOn());

        service.write(table(), List.of(new TableProperty("description", "Rates by the hour")));

        assertEquals("Explanatory Text", styleAt(TITLES, DAY), "The property stands under the header");
        assertEquals("Heading 3", styleAt(TITLES + 1, DAY));
        assertEquals("Input", styleAt(FIRST_RULE + 1, DAY));
        assertEquals(BorderStyle.HAIR, lineAbove(GROUPED_RULE + 1, DAY));
    }

    @Test
    void formatsACopyInTheRolesOfItsSource() throws IOException {
        var copier = new TableCopyServiceImpl(mock(SystemPropertiesService.class), new TableVersionService(),
                formattingOn());
        var grid = TableTestProjects.sheetGrid(dir, SHEET);

        copier.copyInto(table(), "Rate2", null, grid, model.getTableSyntaxNodes());
        TableWriter.saveWorkbook(grid);

        var copy = TableTestProjects.table(TableTestProjects.projectModel(dir), "Rate2").getGridTable().getRegion();
        assertEquals("Title", styleAt(copy.getTop(), copy.getLeft()));
        assertEquals("40% - Accent1", styleAt(copy.getTop() + 1, copy.getLeft() + 2));
        assertEquals("Input", styleAt(copy.getTop() + 2, copy.getLeft() + 1));
        assertEquals(BorderStyle.HAIR, lineAbove(copy.getTop() + 4, copy.getLeft() + 1), "The group of the source");
    }

    @Test
    void formatsATableCreatedInTheModuleInTheRegionsItsBuilderSent() throws IOException {
        var formatting = formattingOn();
        var creator = new TableCreatorService(new TableWritersFactory(formatting), new TableWriterExecutor(), null,
                null, mock(SystemPropertiesService.class), formatting);
        var view = RawTableView.builder()
                .kind(TableKind.DATATYPE)
                .name("Person")
                .source(List.of(
                        List.of(cell("Datatype Person"), RawTableCell.COVERED_CELL),
                        List.of(cell("String"), cell("name")),
                        List.of(cell("Integer"), cell("age"))))
                .build();
        var regions = List.of(
                new RawTableRegion(RawTableRole.SIGNATURE, null, 0, 0, 0, 1),
                new RawTableRegion(RawTableRole.CODE, null, 1, 0, 2, 0),
                new RawTableRegion(RawTableRole.KEY, null, 1, 1, 2, 1));

        creator.createTable(new CreateNewTableRequest("Rules", SHEET, null, view, regions), model);

        var person = TableTestProjects.table(TableTestProjects.projectModel(dir), "Person").getGridTable().getRegion();
        assertEquals("Title", styleAt(person.getTop(), person.getLeft()));
        assertEquals("Calculation", styleAt(person.getTop() + 2, person.getLeft()));
        assertEquals("Heading 4", styleAt(person.getTop() + 1, person.getLeft() + 1));
    }

    @Test
    void armsEveryWriterOfATableTheFactoryHandsOut() throws IOException {
        var formatting = formattingOn();

        var writer = new TableWritersFactory(formatting).getTableWriter(table(), RawTableView.TABLE_TYPE);
        ((RawTableWriter) writer).apply(List.of(update(FIRST_RULE - HEADER, DAY, "Holiday")));

        verify(formatting).onSave();
        assertEquals("Input", styleAt(FIRST_RULE, DAY));
    }

    private static TableFormatting formattingOn() {
        var formatting = mock(TableFormatting.class);
        when(formatting.onSave()).thenReturn(true);
        return formatting;
    }

    private static RawTableCell cell(Object value) {
        return RawTableCell.builder().value(value).build();
    }

    private void save(boolean format, RawTableSourceAction action) {
        save(table(), format, action);
    }

    private static void save(IOpenLTable table, boolean format, RawTableSourceAction action) {
        var writer = new RawTableWriter(table);
        writer.formatOnSave(format);
        writer.apply(List.of(action));
    }

    private IOpenLTable table() {
        return TableTestProjects.table(model, "Rate");
    }

    private String styleAt(int row, int column) throws IOException {
        return styleNameAt(dir.resolve(SHEET + ".xlsx"), SHEET, row, column);
    }

    private BorderStyle lineAbove(int row, int column) throws IOException {
        return TableTestProjects.lineAbove(dir.resolve(SHEET + ".xlsx"), SHEET, row, column);
    }

    private static RawTableSourceAction update(int row, int column, String value) {
        return new RawTableSourceAction.Update(new UpdateTarget.Cell(row, column - HOUR, value));
    }

    private static RawTableSourceAction insertRow(int position, String... values) {
        var cells = Arrays.stream(values).map(value -> new RawCellInput(value, null, null, null)).toList();
        return new RawTableSourceAction.Insert(new InsertTarget.Rows(position, List.of(cells)));
    }

    /** A SmartRules table whose first two rules share the hour, which makes them a group. */
    private static void fillSheet(Sheet sheet) {
        row(sheet, HEADER, HOUR, "SmartRules Double Rate(Integer hour, String day)");
        merge(sheet, HEADER, HEADER, HOUR, RATE);
        row(sheet, TITLES, HOUR, "Hour", "Day", "Rate");
        row(sheet, FIRST_RULE, HOUR, "< 12", "Weekday", "1.5");
        row(sheet, FIRST_RULE + 1, HOUR, null, "Weekend", "2");
        merge(sheet, FIRST_RULE, FIRST_RULE + 1, HOUR, HOUR);
        row(sheet, GROUPED_RULE, HOUR, ">= 12", "Weekday", "1");
    }
}
