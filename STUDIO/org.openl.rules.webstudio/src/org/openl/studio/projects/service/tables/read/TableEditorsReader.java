package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.event.TableEditorController.EditorTypeResponse;
import org.openl.rules.tableeditor.model.ArrayCellEditor.ArrayEditorParams;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ComboBoxCellEditor.ComboBoxParam;
import org.openl.rules.tableeditor.model.ICellEditor;
import org.openl.rules.tableeditor.model.MultiSelectCellEditor.MultiChoiceParam;
import org.openl.rules.tableeditor.model.NumberRangeEditor.NumberRangeParams;
import org.openl.rules.tableeditor.model.RangeParam;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
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

    private static final int NO_ROW_CAP = -1;

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
        var gridTable = sliceFrom(openLTable.getGridTable(), startRow);
        var tableModel = gridTable == null ? null
                : TableModel.initializeTableModel(gridTable, maxRows == null ? NO_ROW_CAP : maxRows, metaInfoReader);
        if (tableModel == null) {
            return new TableEditorsView(List.of(), List.of());
        }
        return collect(tableModel, metaInfoReader, maxRows);
    }

    /** Walks the window's cells, keeping each editor once and pointing every cell that asks for it at that one. */
    private TableEditorsView collect(TableModel tableModel, MetaInfoReader metaInfoReader,
            @Nullable Integer maxRows) {
        Map<TableCellEditorView, Integer> editors = new LinkedHashMap<>();
        var cells = new ArrayList<TableEditorsView.Cell>();
        var grid = tableModel.getGridTable();
        var height = maxRows == null ? tableModel.getHeight() : Math.min(tableModel.getHeight(), maxRows);
        var width = tableModel.getHeight() > 0 ? tableModel.getCells()[0].length : 0;
        var covered = new HashSet<CellRef>();
        for (var row = 0; row < height; row++) {
            for (var column = 0; column < width; column++) {
                // A cell a merge reaches over holds nothing of its own — the raw read reports it as covered, and
                // the grid model stands one in for the cell it belongs to rather than describing it.
                if (!covered.contains(new CellRef(row, column))
                        && tableModel.getCells()[row][column] instanceof CellModel cellModel) {
                    markCovered(covered, row, column, cellModel, height, width);
                    var cell = grid.getCell(cellModel.getColumn(), cellModel.getRow());
                    add(cells, row, column, editorOf(cell, metaInfoReader), editors);
                }
            }
        }
        return new TableEditorsView(List.copyOf(editors.keySet()), cells);
    }

    /** Points the cell at the editor it asks for, keeping that editor once for the whole table. */
    private static void add(List<TableEditorsView.Cell> cells, int row, int column,
            @Nullable TableCellEditorView editor, Map<TableCellEditorView, Integer> editors) {
        if (editor == null) {
            return;
        }
        var next = editors.size();
        var kept = editors.putIfAbsent(editor, next);
        cells.add(new TableEditorsView.Cell(row, column, kept == null ? next : kept));
    }

    /** Notes the cells a merged one reaches over, so each of them is passed by rather than read. */
    private static void markCovered(Set<CellRef> covered, int row, int col, CellModel cellModel,
            int height, int width) {
        var lastRow = Math.min(row + cellModel.getRowspan(), height);
        var lastCol = Math.min(col + cellModel.getColspan(), width);
        for (var r = row; r < lastRow; r++) {
            for (var c = col; c < lastCol; c++) {
                if (r > row || c > col) {
                    covered.add(new CellRef(r, c));
                }
            }
        }
    }

    /** The editor the cell asks for, or {@code null} when it takes plain text like any other cell. */
    private @Nullable TableCellEditorView editorOf(ICell cell, MetaInfoReader metaInfoReader) {
        var metaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        if (metaInfo == null) {
            return null;
        }
        var selected = selector.selectEditor(cell, metaInfo);
        return selected == null ? null : describe(selected.getEditorTypeAndMetadata());
    }

    /** The editor and its parameters as the API reports them, or {@code null} for the ones a screen knows itself. */
    private static @Nullable TableCellEditorView describe(EditorTypeResponse response) {
        var kind = response.getEditor();
        if (ICellEditor.CE_TEXT.equals(kind)
                || ICellEditor.CE_MULTILINE.equals(kind)
                || ICellEditor.CE_FORMULA.equals(kind)) {
            return null;
        }
        var described = TableCellEditorView.builder().editor(kind);
        switch (response.getParams()) {
            case MultiChoiceParam params -> described.choices(List.of(params.getChoices()))
                    .displayValues(displayValues(params))
                    .separator(params.getSeparator())
                    .separatorEscaper(params.getSeparatorEscaper());
            case ComboBoxParam params -> described.choices(List.of(params.getChoices()))
                    .displayValues(displayValues(params));
            case RangeParam params -> described.min(params.getMin())
                    .max(params.getMax())
                    .intOnly(params.isIntOnly());
            case ArrayEditorParams params -> described.separator(params.getSeparator())
                    .entryEditor(params.getEntryEditor())
                    .intOnly(params.isIntOnly());
            case NumberRangeParams params -> described.entryEditor(params.getEntryEditor());
            case null, default -> {
                // A date or a boolean is entered the same way wherever it stands, so it carries nothing.
            }
        }
        return described.build();
    }

    /** What to show for each choice, or the choices themselves when the domain gives no other wording. */
    private static @Nullable List<String> displayValues(ComboBoxParam params) {
        return params.getDisplayValues() == null ? null : List.of(params.getDisplayValues());
    }

    /** The table's meta info, or an empty one when the table carries none. */
    private static MetaInfoReader metaInfoReaderOf(IOpenLTable openLTable) {
        var metaInfoReader = openLTable.getSyntaxNode().getMetaInfoReader();
        return metaInfoReader == null ? EmptyMetaInfoReader.getInstance() : metaInfoReader;
    }

    /** The table from {@code startRow} down, or {@code null} when the window starts past its last row. */
    private static @Nullable IGridTable sliceFrom(IGridTable table, @Nullable Integer startRow) {
        if (startRow == null || startRow <= 0) {
            return table;
        }
        if (startRow >= table.getHeight()) {
            return null;
        }
        return table.getRows(startRow);
    }
}
