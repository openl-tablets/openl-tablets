package org.openl.studio.projects.service.tables.write;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableIds;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriterImpl;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.CellKey.CellKeyFactory;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.SubGridTable;
import org.openl.rules.table.actions.GridRegionAction;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.actions.MergeCellsAction;
import org.openl.rules.table.actions.OtherTables;
import org.openl.rules.table.actions.RemoveMergedRegionsAction;
import org.openl.rules.table.actions.UndoableCompositeAction;
import org.openl.rules.table.actions.UndoableEditTableAction;
import org.openl.rules.table.actions.UndoableInsertColumnsAction;
import org.openl.rules.table.actions.UndoableInsertRowsAction;
import org.openl.rules.table.actions.UndoableRemoveColumnsAction;
import org.openl.rules.table.actions.UndoableRemoveRowsAction;
import org.openl.rules.table.actions.UndoableSetValueAction;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.format.ExcelTableFormatter;
import org.openl.studio.projects.service.tables.format.FormattedTables;
import org.openl.studio.projects.service.tables.format.RegionShift;
import org.openl.studio.projects.service.tables.format.TableRegions;
import org.openl.studio.projects.service.tables.read.CellValueReader;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Base class for table writers.
 *
 * @author Vladyslav Pikus
 */
public abstract class TableWriter<T extends TableView> {

    protected static final int NUMBER_PROPERTIES_COLUMNS = 3;

    protected final IOpenLTable table;
    protected final IGridTable originalTable;
    /**
     * The tables beside this one, found once for every line the write inserts: finding them reads the whole sheet.
     */
    @Getter(AccessLevel.PACKAGE)
    private final OtherTables otherTables;
    private MetaInfoWriter metaInfoWriter;
    private Map<String, Object> stamped = Map.of();
    private Map<String, Object> stampedOnEdit = Map.of();

    /** The regions the save formats the table in, moved along the lines the write inserts and removes. */
    private @Nullable RegionShift layout;

    /**
     * Whether tables are formatted on save, which leaves a write no styling of its own but the indent. It holds even
     * for a table the save does not format this time, as one whose module waits for a compile.
     */
    @Getter(AccessLevel.PROTECTED)
    private boolean formattingOnSave;


    /**
     * The module a new table is written to, as last compiled: the new table is numbered among its tables. Absent for
     * a table that was there before the write, which knows its module.
     */
    @Setter
    private @Nullable XlsModuleSyntaxNode module;

    /** The id of the table as the save left it, read while the table was still open for writing. */
    private @Nullable String writtenId;

    protected TableWriter(IOpenLTable table) {
        this.table = table;
        this.originalTable = GridTableUtils.getOriginalTable(table.getGridTable());
        this.otherTables = new OtherTables(originalTable);
    }

    protected TableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        this.originalTable = gridTable;
        this.metaInfoWriter = metaInfoWriter;
        this.table = null;
        this.otherTables = new OtherTables(originalTable);
    }

    public void write(T tableView) {
        try {
            getGridTable().edit();
            updateBusinessBody(tableView);
            updateTableProperties(recorded(tableView.properties));
            updateHeader(tableView);
            save();
        } finally {
            getGridTable().stopEditing();
        }
    }

    /**
     * Records what OpenL Studio notes about a table it writes, such as its author and creation date.
     *
     * <p>The properties are written by the same pass that writes the table, so the workbook is saved once.
     *
     * @param properties the properties to record, in the order they are written
     */
    public void stampWith(Map<String, Object> properties) {
        this.stamped = properties;
    }

    /**
     * Records what OpenL Studio notes about an edit of the table: who made it and when.
     *
     * <p>Written by the pass that writes the change itself, so the workbook is saved once and the note cannot
     * end up disagreeing with the edit it is about.
     *
     * <p>Unlike what is noted when a table is created, these replace whatever the table carried before: they
     * answer for the last edit rather than for the first.
     *
     * @param properties the properties to record, in the order they are written
     */
    public void stampEditWith(Map<String, Object> properties) {
        this.stampedOnEdit = properties;
    }

    /**
     * Formats the table with the OpenL table style when it is saved, with no compile. Called before the write.
     *
     * <p>The table is formatted in the regions its last compile laid it out in, moved along the lines the write
     * inserts and removes, and its header and properties as they stand once written. A table of the type Other, and a
     * table whose workbook was written to since its last compile, are saved as they are written: what the compile read
     * no longer answers for the sheet.
     *
     * @param format whether the save formats the table
     */
    public void formatOnSave(boolean format) {
        formattingOnSave = format;
        layout = format && isUpdateMode() ? RegionShift.ofCompiled(table) : null;
    }

    /**
     * Formats a table being written for the first time with the OpenL table style when it is saved, in the regions its
     * builder laid it out in. Called before the write.
     *
     * @param regions the regions of the table as its builder wrote it, in table coordinates
     * @param height  how many rows the table the builder wrote has
     * @param width   how many columns the table the builder wrote has
     */
    public void formatOnSave(TableRegions regions, int height, int width) {
        formattingOnSave = true;
        layout = RegionShift.of(regions, height, width);
    }

    /**
     * Tells the layout the save formats the table in that lines were inserted.
     *
     * @param rows  whether rows were inserted, rather than columns
     * @param at    the line of the table the first inserted line takes
     * @param count how many lines were inserted
     */
    protected void linesInserted(boolean rows, int at, int count) {
        if (layout != null) {
            if (rows) {
                layout.rowsInserted(at, count);
            } else {
                layout.columnsInserted(at, count);
            }
        }
    }

    /**
     * Tells the layout the save formats the table in that the cells of rows were restyled, so that a table formatted
     * before has them formatted again.
     *
     * @param at    the row of the table the first restyled cell stands in
     * @param count how many rows hold restyled cells
     */
    protected void rowsRestyled(int at, int count) {
        if (layout != null) {
            layout.rowsRestyled(at, count);
        }
    }

    /**
     * Formats the table in the regions of its layout, as it stands once written: only the lines the write touched
     * when its previous save left it formatted. A formatting that fails leaves the table as it is written, so the
     * edit is saved all the same.
     *
     * @param position where the table stands, read while it is open for writing
     * @return the regions the table stands formatted in, or {@code null} when it is saved unformatted
     */
    private @Nullable TableRegions formatTable(FormattedTables.@Nullable Position position) {
        return layout == null ? null : ExcelTableFormatter.formatOnSave(originalTable, layout,
                FormattedTables.isFormatted(position, layout.start()));
    }

    /**
     * The properties to write: what OpenL Studio records about the table, then what the table itself declares.
     *
     * <p>What the table says wins — a property it declares is written as it stands, whether it declares it as one of
     * its own properties or as a row of the body already written.
     */
    private Map<String, Object> recorded(Map<String, Object> declared) {
        if (stamped.isEmpty()) {
            return declared;
        }
        var region = originalTable.getRegion();
        var grid = originalTable.getGrid();
        var properties = new LinkedHashMap<>(stamped);
        properties.keySet().removeIf(name -> GridTool.getPropertyRowIndex(region, grid, name) != -1);
        properties.putAll(declared);
        return properties;
    }

    /**
     * Removes the table from the sheet.
     * <p>
     * Clears the merged regions and the content of every cell in the table area, then saves the workbook. The table no
     * longer exists once the project is recompiled, and the tables that share what it is keep their ids.
     */
    public void delete() {
        TableIds.deleting(table.getSyntaxNode());
        var gridTable = getGridTable(IXlsTableNames.VIEW_DEVELOPER);
        try {
            gridTable.edit();
            clearMergedRegions(gridTable);
            clearCells(gridTable);
            // The table is meant to be gone, so the area it occupied is left blank on purpose — saved past the
            // blank-line guard that every write goes through.
            saveWorkbook((IWritableGrid) gridTable.getGrid());
        } finally {
            gridTable.stopEditing();
        }
    }

    private void clearCells(IGridTable gridTable) {
        var grid = (IWritableGrid) gridTable.getGrid();
        var region = gridTable.getRegion();
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            for (var col = region.getLeft(); col <= region.getRight(); col++) {
                grid.clearCell(col, row);
            }
        }
    }

    /**
     * Returns the identifier of the table as written: the one it is known by once its module is compiled again.
     * <p>
     * A table that is still what it was keeps its identifier, also when the write moved it because there was no room
     * to grow at its place. A write that renames the table, or changes its {@code id} property, gives it a new one.
     * Call after the write completes.
     *
     * @return the identifier of the written table
     */
    public String getTableId() {
        return writtenId != null ? writtenId : idAsWritten();
    }

    /** The id of the table as it stands in the grid, which only a writer still writing reads as written. */
    private String idAsWritten() {
        return table == null
                ? TableIds.idOfWritten(originalTable, module)
                : TableIds.idAfterWrite(table.getSyntaxNode(), originalTable);
    }

    protected abstract void updateHeader(T tableView);

    protected abstract void updateBusinessBody(T tableView);

    protected void updateTableProperties(Map<String, Object> properties) {
        var originalRegion = originalTable.getRegion();
        var originalGrid = originalTable.getGrid();
        for (var entry : properties.entrySet().stream().toList().reversed()) {
            var propName = entry.getKey();
            var newProperty = GridTool.getPropertyRowIndex(originalRegion, originalGrid, propName) == -1;
            if (newProperty) {
                var tableWidth = GridRegionUtils.width(originalRegion);
                var nColsToInsert = 0;
                if (tableWidth < NUMBER_PROPERTIES_COLUMNS) {
                    nColsToInsert = NUMBER_PROPERTIES_COLUMNS - tableWidth;
                }
                if (!UndoableInsertRowsAction.canInsertRows(originalTable, 1, otherTables)
                        || !UndoableInsertColumnsAction.canInsertColumns(originalTable, nColsToInsert, otherTables)) {
                    UndoableEditTableAction.moveTable(originalTable, getMetaInfoWriter());
                }
                var allTable = new GridRegionAction(originalRegion,
                        UndoableEditTableAction.ROWS,
                        UndoableEditTableAction.INSERT,
                        GridRegionAction.ActionType.EXPAND,
                        1);
                allTable.doAction(originalTable);
            }
            var propValue = entry.getValue();
            var action = GridTool.insertProp(originalRegion, originalGrid, propName, propValue, getMetaInfoWriter());
            if (action != null) {
                action.doAction(originalTable);
            }
        }
    }

    /**
     * Creates or updates cell in the table.
     *
     * <p>A cell past the bottom or the right edge grows the table to reach it: the rows or the columns between are
     * inserted at that edge. A cell inside the table, the last row and the last column included, leaves its size as
     * it is.
     *
     * <p>So a line written past the edge grows the table once, by its first cell.
     *
     * <p>On update, a cell that already holds the value is left as it is: written again, a number kept as a text
     * would become a number, and the table would change where nobody changed it.
     *
     * @param gridTable table to update
     * @param cellKey   relative cell coordinates
     * @param value     new cell value
     */
    protected void createOrUpdateCell(IGridTable gridTable, CellKey cellKey, Object value) {
        if (isUpdateMode() && holds(gridTable, cellKey, value)) {
            return;
        }
        var at = reach(gridTable, cellKey);
        updateCellValue(gridTable, at.column(), at.row(), value);
    }

    /**
     * Writes an Excel formula into a cell, such as {@code =B2*C2}, growing the table to reach it as
     * {@link #createOrUpdateCell} does.
     *
     * <p>On update, a cell that already holds the formula is left as it is, with the value it computed. The cell
     * keeps the type it was read with, and forgets what its previous text referred to.
     *
     * @param gridTable table to update
     * @param cellKey   relative cell coordinates
     * @param formula   the formula, starting with {@code =}
     * @throws BadRequestException when the formula does not start with {@code =}, Excel cannot read it or OpenL
     *                             Studio cannot compute it
     */
    protected void createOrUpdateFormula(IGridTable gridTable, CellKey cellKey, String formula) {
        var excelFormula = excelFormulaOf(formula);
        if (isUpdateMode() && holdsFormula(gridTable, cellKey, excelFormula)) {
            return;
        }
        var at = reach(gridTable, cellKey);
        try {
            ((IWritableGrid) gridTable.getGrid()).setCellFormula(at.column(), at.row(), excelFormula);
        } catch (IllegalArgumentException e) {
            throw invalidFormula(formula);
        }
        var metaInfo = getMetaInfoWriter().getMetaInfo(at.row(), at.column());
        if (metaInfo != null) {
            getMetaInfoWriter().setMetaInfo(at.row(), at.column(),
                    new CellMetaInfo(metaInfo.getDataType(), metaInfo.isMultiValue()));
        }
    }

    /**
     * The Excel formula a request names, such as {@code =B2*C2}, without its leading {@code =}.
     *
     * @throws BadRequestException when the formula does not start with {@code =} or holds nothing after it
     */
    public static String excelFormulaOf(String formula) {
        var text = formula.strip();
        var excelFormula = text.startsWith("=") ? text.substring(1).strip() : "";
        if (excelFormula.isEmpty()) {
            throw invalidFormula(formula);
        }
        return excelFormula;
    }

    /** The refusal of an Excel formula that Excel cannot read or OpenL Studio cannot compute. */
    public static BadRequestException invalidFormula(String formula) {
        return new BadRequestException("table.cell.formula.invalid.message", new Object[]{formula});
    }

    /** A cell of the sheet, by its column and its row. */
    private record SheetCell(int column, int row) {
    }

    /**
     * The cell of the sheet at a table position. A position past the bottom or the right edge grows the table to
     * reach it.
     */
    private SheetCell reach(IGridTable gridTable, CellKey cellKey) {
        var region = gridTable.getRegion();
        // calculate absolute coordinates
        var grow = cellKey.getRow() + region.getTop();
        // Check if the cell is out of the table. Every insert looks over the whole sheet for room, so a cell inside
        // the table must not ask for one: a column of a large table would cost a pass over the sheet per row.
        var inserted = false;
        if (region.getBottom() < grow) {
            var nRows = grow - region.getBottom();
            var beforeRow = region.getTop() - originalTable.getRegion().getTop() + GridRegionUtils.height(region);
            insertRows(gridTable, nRows, beforeRow);
            linesInserted(true, beforeRow, nRows);
            inserted = true;
        }
        var gcol = cellKey.getColumn() + region.getLeft();
        if (region.getRight() < gcol) {
            var nCols = gcol - region.getRight();
            var beforeCol = region.getLeft() - originalTable.getRegion().getLeft() + GridRegionUtils.width(region);
            insertColumns(gridTable, nCols, beforeCol);
            linesInserted(false, beforeCol, nCols);
            inserted = true;
        }
        if (inserted) {
            // recalculate absolute coordinates because it may be changed after inserting columns
            grow = cellKey.getRow() + region.getTop();
            gcol = cellKey.getColumn() + region.getLeft();
        }
        return new SheetCell(gcol, grow);
    }

    /**
     * Tells whether the cell at a table position already holds the value a write would put there: the text it shows,
     * or the value it reads as.
     */
    private boolean holds(IGridTable gridTable, CellKey cellKey, @Nullable Object value) {
        var cell = ownCell(gridTable, cellKey);
        return cell != null && (value instanceof String text && text.equals(cell.getStringValue())
                || sameValue(new CellValueReader(getMetaInfoWriter()).apply(cell), value));
    }

    /**
     * Empties the cell at a table position when it holds an Excel formula, keeping its style.
     *
     * <p>A value written into a cell holding an Excel formula would keep the formula and change only the value it
     * computed, so a write that replaces the formula empties the cell first.
     */
    protected void dropFormula(IGridTable gridTable, CellKey cellKey) {
        var cell = ownCell(gridTable, cellKey);
        if (cell != null && cell.getFormula() != null) {
            ((IWritableGrid) gridTable.getGrid()).setCellValue(cell.getAbsoluteColumn(), cell.getAbsoluteRow(), null);
        }
    }

    /** Tells whether the cell at a table position already holds the Excel formula, written without its {@code =}. */
    private static boolean holdsFormula(IGridTable gridTable, CellKey cellKey, String formula) {
        var cell = ownCell(gridTable, cellKey);
        return cell != null && formula.equals(cell.getFormula());
    }

    /**
     * The cell at a table position, or {@code null} where it holds nothing of its own yet. A position outside the
     * table holds nothing yet, and nor does a cell a merged cell covers: it reads as the value of the merged cell,
     * which a write that unmerges it must not take for its own.
     */
    private static @Nullable ICell ownCell(IGridTable gridTable, CellKey cellKey) {
        if (cellKey.getRow() >= gridTable.getHeight() || cellKey.getColumn() >= gridTable.getWidth()) {
            return null;
        }
        var region = gridTable.getRegion();
        var column = region.getLeft() + cellKey.getColumn();
        var row = region.getTop() + cellKey.getRow();
        var merged = gridTable.getGrid().getRegionContaining(column, row);
        if (merged != null && (merged.getLeft() != column || merged.getTop() != row)) {
            return null;
        }
        return gridTable.getCell(cellKey.getColumn(), cellKey.getRow());
    }

    /** Whether two cell values are the same: numbers by their value, arrays and lists element by element. */
    static boolean sameValue(@Nullable Object current, @Nullable Object value) {
        if (current instanceof Number a && value instanceof Number b) {
            try {
                return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString())) == 0;
            } catch (NumberFormatException e) {
                return a.equals(b);
            }
        }
        var currentItems = itemsOf(current);
        var items = itemsOf(value);
        if (currentItems != null && items != null) {
            if (currentItems.size() != items.size()) {
                return false;
            }
            for (var i = 0; i < items.size(); i++) {
                if (!sameValue(currentItems.get(i), items.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return Objects.equals(current, value);
    }

    /** The items of an array or a collection value, or {@code null} for a single value. */
    private static @Nullable List<?> itemsOf(@Nullable Object value) {
        return switch (value) {
            case Object[] array -> Arrays.asList(array);
            // An item may be blank, which List.copyOf refuses
            case Collection<?> collection -> new ArrayList<>(collection);
            case null, default -> null;
        };
    }

    /** Grows the table by the columns a cell written past its right edge needs. */
    void insertColumns(IGridTable gridTable, int nCols, int beforeCol) {
        new UndoableInsertColumnsAction(nCols, beforeCol, getMetaInfoWriter(), otherTables).doAction(gridTable);
    }

    /** Grows the table by the rows a cell written past its bottom edge needs. */
    void insertRows(IGridTable gridTable, int nRows, int beforeRow) {
        new UndoableInsertRowsAction(nRows, beforeRow, getMetaInfoWriter(), otherTables).doAction(gridTable);
    }

    @SuppressWarnings("rawtypes")
    private void updateCellValue(IGridTable gridTable, int gcol, int grow, Object value) {
        if (value instanceof Collection collection) {
            // OpenL table cell can store only array, not collection
            value = collection.toArray();
        }
        new UndoableSetValueAction(gcol, grow, value, getMetaInfoWriter()).doAction(gridTable);
    }

    protected void removeRows(IGridTable gridTable, int nRows, int startRow) {
        var startRow0 = gridTable.getRegion().getTop() - originalTable.getRegion().getTop() + startRow;
        var height = GridRegionUtils.height(originalTable.getRegion());
        new UndoableRemoveRowsAction(nRows, startRow0, getMetaInfoWriter()).doAction(gridTable);
        if (layout != null) {
            // A merged line takes the lines merged with it along: the table says how many went.
            layout.rowsRemoved(startRow0, height - GridRegionUtils.height(originalTable.getRegion()));
        }
    }

    protected void removeColumns(IGridTable gridTable, int nCols, int startCol) {
        var startCol0 = gridTable.getRegion().getLeft() - originalTable.getRegion().getLeft() + startCol;
        var width = GridRegionUtils.width(originalTable.getRegion());
        new UndoableRemoveColumnsAction(nCols, startCol0, getMetaInfoWriter()).doAction(gridTable);
        if (layout != null) {
            layout.columnsRemoved(startCol0, width - GridRegionUtils.width(originalTable.getRegion()));
        }
    }

    /**
     * Verifies that an appended row does not widen the table. Appending lines must not introduce new columns.
     *
     * @param appendedColumns number of columns in the appended row
     * @param tableColumns    number of columns the table currently has
     * @throws BadRequestException if the appended row has more columns than the table
     */
    protected void requireColumnsWithinTable(int appendedColumns, int tableColumns) {
        if (appendedColumns > tableColumns) {
            throw new BadRequestException("table.append.column.count.message");
        }
    }

    protected MetaInfoWriter getMetaInfoWriter() {
        if (metaInfoWriter == null && isUpdateMode()) {
            metaInfoWriter = new MetaInfoWriterImpl(table.getMetaInfoReader(), table.getGridTable());
        }
        return metaInfoWriter;
    }

    /** Persist the table that was written, refusing to leave it split by a blank line. */
    protected void save() {
        recordEdit();
        requireNoBlankLine();
        // Read while the table is open for writing: the save closes it.
        var position = layout == null ? null : FormattedTables.positionOf(originalTable);
        var formatted = formatTable(position);
        // Read while the table is open for writing too: once saved, the sheet is read as it was compiled.
        writtenId = idAsWritten();
        var grid = (IWritableGrid) getGridTable().getGrid();
        if (grid instanceof XlsSheetGridModel workbook) {
            // A Markdown file holds no Excel formulas to compute
            PoiExcelHelper.evaluateFormulas(workbook.getSheetToWrite(), getGridTable().getRegion());
        }
        saveWorkbook(grid);
        if (formatted != null) {
            FormattedTables.formatted(position, formatted);
        }
    }

    /**
     * Writes onto the table what OpenL Studio notes about the edit being saved.
     *
     * <p>Nothing is written where the installation records nothing, and nothing where the table has nowhere to
     * write it — a table of a kind that carries no properties, and a table being laid down for the first time,
     * which is a creation and noted as one.
     */
    protected void recordEdit() {
        if (table != null && isUpdateMode()) {
            recordEdit(table, stampedOnEdit);
        }
    }

    /**
     * Writes onto a table what OpenL Studio notes about an edit of it, such as who made it and when.
     *
     * <p>Nothing is written where the installation records nothing, and nothing onto a table of a kind that carries no
     * properties. A table without room for the note moves to where it has room.
     *
     * @param table      the table edited, opened for writing
     * @param properties what is noted about the edit, in the order it is written; empty when nothing is
     */
    public static void recordEdit(IOpenLTable table, Map<String, Object> properties) {
        if (properties.isEmpty() || !table.isCanContainProperties()) {
            return;
        }
        var editor = new TableEditorModel(table);
        properties.forEach(editor::setProperty);
    }

    /**
     * Rejects a write that leaves a blank row or a blank column inside the table.
     *
     * <p>OpenL reads a table as the block of filled cells reachable from its top-left corner, so a blank line
     * inside it ends the table there. Everything beyond that line stops being part of the table and never
     * compiles, while the write itself reports success.
     *
     * <p>A cell a merge spans into counts as filled, so a line that a merge declared elsewhere legitimately
     * crosses is not blank.
     */
    private void requireNoBlankLine() {
        var view = getGridTable(IXlsTableNames.VIEW_DEVELOPER);
        var region = view.getRegion();
        var grid = view.getGrid();
        IntPredicate rowFilled = row -> IntStream.rangeClosed(region.getLeft(), region.getRight())
                .anyMatch(column -> GridSplitter.containsCell(grid, column, row));
        IntPredicate columnFilled = column -> IntStream.rangeClosed(region.getTop(), region.getBottom())
                .anyMatch(row -> GridSplitter.containsCell(grid, column, row));
        if (hasBlankLineInside(region.getTop(), region.getBottom(), rowFilled)
                || hasBlankLineInside(region.getLeft(), region.getRight(), columnFilled)) {
            throw new BadRequestException("table.action.line.all-empty.message");
        }
    }

    /**
     * Whether a blank line sits between two filled ones, which is what splits a table.
     *
     * <p>Only the block between the first and the last filled line counts. Blank lines around the table leave it
     * smaller than the area it was written into, which loses nothing.
     *
     * <p>Shared with the writer that lays a table into a workbook of its own, which has no grid to scan and finds
     * its filled lines over the sheet instead.
     *
     * @param filled tells whether the line at the given index carries content
     */
    public static boolean hasBlankLineInside(int from, int to, IntPredicate filled) {
        var lines = IntStream.rangeClosed(from, to).filter(filled).summaryStatistics();
        return lines.getCount() > 0 && lines.getMax() - lines.getMin() + 1 > lines.getCount();
    }

    /**
     * Persist the file the grid belongs to: its workbook, or the text of a Markdown file.
     *
     * <p>Writes what the grid holds as it stands. A caller writing a table saves through {@link #save()} instead,
     * which first refuses a table left split by a blank line.
     */
    public static void saveWorkbook(IWritableGrid grid) {
        try {
            grid.save();
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    protected CellKey buildCellKey(int col, int row) {
        return CellKeyFactory.getCellKey(col, row);
    }

    protected String getBusinessTableType(T tableView) {
        return isUpdateMode()
                ? OpenLTableUtils.getTableTypeItems().get(table.getType())
                : tableView.tableType;
    }

    /**
     * Remove the merged regions that currently belong to the table.
     * <p>
     * Counterpart of {@link #applyMergeRegions}, which only adds regions. Call this before re-applying merges from a
     * fresh source so that merges dropped from the source do not linger in the grid.
     *
     * @param gridTable The grid table whose merged regions are cleared
     */
    protected void clearMergedRegions(IGridTable gridTable) {
        new RemoveMergedRegionsAction(gridTable.getRegion()).doAction(gridTable);
    }

    /**
     * Apply merge regions to the grid.
     * <p>
     * Creates MergeCellsAction for each merge region and executes them.
     * This should be called after all cell values are written to ensure
     * grid dimensions are correct.
     *
     * @param gridTable    The grid table to apply merges to
     * @param mergeRegions The list of regions to merge
     */
    protected void applyMergeRegions(IGridTable gridTable, List<IGridRegion> mergeRegions) {
        var tableRegion = gridTable.getRegion();
        List<IUndoableGridTableAction> mergeActions = mergeRegions.stream()
                .map(mr -> toAbsolute(mr, tableRegion))
                .map(MergeCellsAction::new)
                .collect(Collectors.toList());

        if (!mergeActions.isEmpty()) {
            new UndoableCompositeAction(mergeActions).doAction(gridTable);
        }
    }

    private IGridRegion toAbsolute(IGridRegion mr, IGridRegion tableRegion) {
        var top = mr.getTop() + tableRegion.getTop();
        var left = mr.getLeft() + tableRegion.getLeft();
        var bottom = mr.getBottom() + tableRegion.getTop();
        var right = mr.getRight() + tableRegion.getLeft();
        return new GridRegion(top, left, bottom, right);
    }

    protected boolean isUpdateMode() {
        return table != null;
    }

    protected IGridTable getGridTable() {
        if (isUpdateMode()) {
            return table.getGridTable();
        }
        return originalTable;
    }

    protected IGridTable getGridTable(String view) {
        if (isUpdateMode()) {
            return IXlsTableNames.VIEW_BUSINESS.equals(view) ? businessBody() : table.getGridTable(view);
        }
        if (IXlsTableNames.VIEW_BUSINESS.equals(view)) {
            return originalTable.getSubtable(0,
                    1,
                    originalTable.getWidth(),
                    originalTable.getHeight() - 1);
        }
        return originalTable;
    }

    /**
     * The body of the table being updated: its rows under the header and the properties.
     *
     * <p>Taken as the engine bound it. A table the engine could not bind has no body view of its own, and the whole
     * grid would stand in for it, header included, to be written over as if it were the body. Such a table is given
     * the rows under its header instead — an empty body under the last row of a table written as a header alone,
     * the way an author leaves it while writing.
     */
    private IGridTable businessBody() {
        var tsn = table.getSyntaxNode();
        var bound = tsn.getTable(IXlsTableNames.VIEW_BUSINESS);
        if (bound != null) {
            return bound.getSource();
        }
        var body = tsn.getTableBody();
        var fromRow = body == null
                ? originalTable.getHeight()
                : body.getSource().getRegion().getTop() - originalTable.getRegion().getTop();
        return bodyBelow(fromRow);
    }

    /**
     * The rows of the table from the given one down, as a sub-table whatever their number.
     *
     * <p>A table that ends above that row has no body yet, and is answered an empty sub-table there: the rows are
     * inserted as cells are written into it, the way a body grows. Answered as a sub-table even for one cell, which
     * would otherwise be a cell of its own with no region to grow.
     */
    protected IGridTable bodyBelow(int fromRow) {
        return new SubGridTable(originalTable, 0, fromRow, originalTable.getWidth(), originalTable.getHeight() - fromRow);
    }

}
