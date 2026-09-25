package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.lang.xls.types.meta.TableArea;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.model.ArrayCellEditor.ArrayEditorParams;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ComboBoxCellEditor.ComboBoxParam;
import org.openl.rules.tableeditor.model.EditorTypeResponse;
import org.openl.rules.tableeditor.model.ICellEditor;
import org.openl.rules.tableeditor.model.MultiSelectCellEditor.MultiChoiceParam;
import org.openl.rules.tableeditor.model.NumberRangeEditor.NumberRangeParams;
import org.openl.rules.tableeditor.model.RangeParam;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.studio.projects.model.tables.DeclaredTableEditorsView;
import org.openl.studio.projects.model.tables.RawTableEditorsView;
import org.openl.studio.projects.model.tables.TableCellEditorView;
import org.openl.studio.projects.model.tables.TableEditorsView;

/**
 * Reads the way each cell of a table is written.
 *
 * <p>The editor a cell asks for follows from what the compiler knows about it: a cell whose type has a domain is
 * chosen from a list, a number is entered within the bounds of its type, a range through its bounds. This is the
 * same choice the legacy editor makes, so the two agree on every table.
 *
 * <p>Cells written as plain text are not reported. Text is what a cell with nothing else to say takes, and a
 * screen that has to decide between plain text and several lines can see that in the value itself — as it must
 * anyway, while a value is being edited and the table has not been written yet.
 *
 * @author Vladyslav Pikus
 */
@Component
public class TableEditorsReader {

    private final CellEditorSelector selector = new CellEditorSelector();

    /**
     * Reads the editors the cells of a window of a table ask for.
     *
     * <p>The window is the {@code maxRows} rows starting at {@code startRow}, matching the window the raw read
     * returns, so a cell is pointed at by the same row and column in both.
     *
     * @param openLTable the table to read
     * @param startRow   the zero-based index of the first row to look at; {@code null} starts at the top
     * @param maxRows    how many rows from {@code startRow} to look at; {@code null} looks at all of them
     * @return the editors the table needs and the cells that ask for them
     */
    public TableEditorsView read(IOpenLTable openLTable, @Nullable Integer startRow, @Nullable Integer maxRows) {
        var metaInfoReader = metaInfoReaderOf(openLTable);
        // The same window the raw read answers with, merged cells whole and all; see TableWindow.
        var window = TableWindow.of(openLTable.getGridTable(), startRow, maxRows);
        var gridTable = sliceFrom(openLTable.getGridTable(), window.startRow());
        var tableModel = gridTable == null ? null
                : TableModel.initializeTableModel(gridTable, window.rows(), metaInfoReader);
        if (tableModel == null) {
            return TableEditorsView.nothing();
        }
        Map<TableCellEditorView, Integer> editors = new LinkedHashMap<>();
        var declared = metaInfoReader.getAreas();
        // A table that declares what a part of it holds answers for the part, and then only for the cells that
        // are written some other way than the part they stand in.
        var taken = byArea(openLTable.getGridTable(), declared, window);
        var cells = collect(tableModel, metaInfoReader, window, taken, editors);
        var view = declared.isEmpty() ? RawTableEditorsView.builder()
                : DeclaredTableEditorsView.builder().areas(taken.stream()
                        .filter(part -> part.editor() != null)
                        .map(part -> new DeclaredTableEditorsView.Area(part.row(),
                                part.column(),
                                part.rows(),
                                part.columns(),
                                indexOf(part.editor(), editors)))
                        .toList());
        return view.editors(List.copyOf(editors.keySet())).cells(cells).build();
    }

    /**
     * What the parts the table declares take, by where each of them stands in the window.
     *
     * <p>A table declares what a part of it holds — the column of a Data table, the condition of a decision
     * table, the cells a lookup's rules meet in — and every cell of that part takes it, the ones nobody has
     * written in yet as much as the ones that hold a value. That is what a line laid down in the table needs
     * to know, and it is all a table holding no rows has to say.
     */
    private List<Taken> byArea(IGridTable table, List<TableArea> declared, TableWindow window) {
        var taken = new ArrayList<Taken>();
        for (var area : declared) {
            var row = area.row() - window.startRow();
            // A part left open runs on past the table's edge, so the window never cuts it short from below.
            var rows = area.rows() == TableArea.TO_THE_END ? null : area.rows();
            if (rows != null && row + rows <= 0) {
                // The window is a window of rows, and this part is wholly above it.
                continue;
            }
            if (row < 0) {
                // A part reaching into the window from above begins, for this reading, at its first row.
                rows = rows == null ? null : rows + row;
                row = 0;
            }
            var columns = area.columns() == TableArea.TO_THE_END ? null : area.columns();
            taken.add(new Taken(row, area.column(), rows, columns, editorOf(cellOf(table, area), area.metaInfo())));
        }
        return taken;
    }

    /**
     * A cell of the part whose editor stands for the whole of it: the first one the table holds there, or the
     * first cell of the part where the table holds nothing there at all.
     */
    private static ICell cellOf(IGridTable table, TableArea area) {
        return table.getCell(Math.clamp(area.column(), 0, table.getWidth() - 1),
                Math.clamp(area.row(), 0, table.getHeight() - 1));
    }

    /**
     * Walks the window's cells, keeping each editor once and pointing every cell that asks for it at that one.
     *
     * <p>A cell written the same way as the part it stands in is passed by: the part has said it already, and a
     * table says the same thing about every cell of a column however many rules are written under it.
     */
    private List<TableEditorsView.Cell> collect(TableModel tableModel, MetaInfoReader metaInfoReader,
            TableWindow window, List<Taken> taken, Map<TableCellEditorView, Integer> editors) {
        var cells = new ArrayList<TableEditorsView.Cell>();
        var grid = tableModel.getGridTable();
        var height = window.rows() == TableWindow.EVERY_ROW ? tableModel.getHeight()
                : Math.min(tableModel.getHeight(), window.rows());
        var width = tableModel.getHeight() > 0 ? tableModel.getCells()[0].length : 0;
        var covered = new CoveredCells(height, width);
        for (var row = 0; row < height; row++) {
            for (var column = 0; column < width; column++) {
                // A cell a merge reaches over holds nothing of its own — the raw read reports it as covered, and
                // the grid model stands one in for the cell it belongs to rather than describing it.
                if (!covered.holds(row, column)
                        && tableModel.getCells()[row][column] instanceof CellModel cellModel) {
                    covered.mark(row, column, cellModel);
                    var cell = grid.getCell(cellModel.getColumn(), cellModel.getRow());
                    var editor = editorOf(cell, metaInfoReader);
                    if (editor != null && !alreadySaid(taken, row, column, editor)) {
                        cells.add(new TableEditorsView.Cell(row, column, indexOf(editor, editors)));
                    }
                }
            }
        }
        return cells;
    }

    /** Where the editor stands among the ones the table needs, keeping it once for the whole table. */
    private static int indexOf(TableCellEditorView editor, Map<TableCellEditorView, Integer> editors) {
        var next = editors.size();
        var kept = editors.putIfAbsent(editor, next);
        return kept == null ? next : kept;
    }

    /**
     * One part of the window and the editor its cells take.
     *
     * @param rows    how many rows it covers, or {@code null} where it runs on past the table's last row
     * @param columns how many columns it covers, or {@code null} where it runs on past the last column
     * @param editor  what every cell of it takes, {@code null} where the part takes plain text
     */
    private record Taken(int row, int column, @Nullable Integer rows, @Nullable Integer columns,
            @Nullable TableCellEditorView editor) {

        boolean holds(int row, int column) {
            return row >= this.row && (rows == null || row < this.row + rows)
                    && column >= this.column && (columns == null || column < this.column + columns);
        }
    }

    /**
     * Whether the part the cell stands in has already said that the cell is written this way.
     *
     * <p>The first part holding the cell answers for it, even where it says the cell takes plain text, so a
     * part laid over another does not let the one underneath speak for it.
     */
    private static boolean alreadySaid(List<Taken> taken, int row, int column, TableCellEditorView editor) {
        for (Taken part : taken) {
            if (part.holds(row, column)) {
                return editor.equals(part.editor());
            }
        }
        return false;
    }

    /** The editor the cell asks for, or {@code null} when it takes plain text like any other cell. */
    private @Nullable TableCellEditorView editorOf(ICell cell, MetaInfoReader metaInfoReader) {
        var metaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        return metaInfo == null ? null : editorOf(cell, metaInfo);
    }

    /** The editor a cell holding what the meta info describes asks for. */
    private @Nullable TableCellEditorView editorOf(ICell cell, CellMetaInfo metaInfo) {
        var selected = selector.selectEditor(cell, metaInfo);
        return selected == null ? null : describe(selected.getEditorTypeAndMetadata());
    }

    /** The editor and its parameters as the API reports them, or {@code null} for the ones a screen knows itself. */
    private static @Nullable TableCellEditorView describe(EditorTypeResponse response) {
        var kind = response.editor();
        if (ICellEditor.CE_TEXT.equals(kind)
                || ICellEditor.CE_MULTILINE.equals(kind)
                || ICellEditor.CE_FORMULA.equals(kind)) {
            return null;
        }
        var described = TableCellEditorView.builder().editor(kind);
        switch (response.params()) {
            case MultiChoiceParam params -> described.choices(List.of(params.choices()))
                    .displayValues(displayValues(params.displayValues()))
                    .separator(params.separator())
                    .separatorEscaper(params.separatorEscaper());
            case ComboBoxParam params -> described.choices(List.of(params.choices()))
                    .displayValues(displayValues(params.displayValues()));
            case RangeParam params -> described.min(params.min())
                    .max(params.max())
                    .intOnly(params.intOnly());
            case ArrayEditorParams params -> described.separator(params.separator())
                    .entryEditor(params.entryEditor())
                    .intOnly(params.intOnly());
            case NumberRangeParams params -> described.entryEditor(params.entryEditor());
            case null, default -> {
                // A date or a boolean is entered the same way wherever it stands, so it carries nothing.
            }
        }
        return described.build();
    }

    /** What to show for each choice, or the choices themselves when the domain gives no other wording. */
    private static @Nullable List<String> displayValues(String @Nullable [] displayValues) {
        return displayValues == null ? null : List.of(displayValues);
    }

    /** The table's meta info, or an empty one when the table carries none. */
    private static MetaInfoReader metaInfoReaderOf(IOpenLTable openLTable) {
        var metaInfoReader = openLTable.getSyntaxNode().getMetaInfoReader();
        return metaInfoReader == null ? EmptyMetaInfoReader.getInstance() : metaInfoReader;
    }

    /** The table from {@code startRow} down, or {@code null} when the window starts past its last row. */
    private static @Nullable IGridTable sliceFrom(IGridTable table, int startRow) {
        if (startRow <= 0) {
            return table;
        }
        if (startRow >= table.getHeight()) {
            return null;
        }
        return table.getRows(startRow);
    }
}
