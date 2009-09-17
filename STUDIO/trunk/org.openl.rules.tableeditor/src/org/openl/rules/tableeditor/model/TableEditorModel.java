/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.tableeditor.model;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.AGridTableDelegator;
import org.openl.rules.table.AUndoableCellAction;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.ICellInfo;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.IUndoGrid;
import org.openl.rules.table.IUndoableAction;
import org.openl.rules.table.IUndoableGridAction;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.UndoableActions;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.FormattedCell;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.XlsUndoGrid;

import java.io.IOException;
import java.util.Vector;

/**
 * @author snshor
 * 
 */
public class TableEditorModel {
    public static enum CellType {
        TH_CELL_TYPE,
        CA_HEADER_CELL_TYPE,
        CA_ENUMERATION_CELL_TYPE
    }

    static class RegionAction implements IUndoableAction {
        IUndoableGridAction gridAction;

        boolean isInsert;
        boolean isColumns;
        int nRowsOrColumns;

        public RegionAction(IUndoableGridAction action, boolean isColumns, boolean isInsert, int nRowsOrColumns) {
            gridAction = action;
            this.isColumns = isColumns;
            this.isInsert = isInsert;
            this.nRowsOrColumns = nRowsOrColumns;
        }

        public void doSome(IGridRegion r, IWritableGrid wgrid, IUndoGrid undoGrid) {
            gridAction.doAction(wgrid, undoGrid);
            updateRegion(isInsert, isColumns, nRowsOrColumns, r);
        }

        public void undoSome(IGridRegion r, IWritableGrid wgrid, IUndoGrid undoGrid) {
            updateRegion(!isInsert, isColumns, nRowsOrColumns, r);
            gridAction.undoAction(wgrid, undoGrid);
        }

        /**
         * @param isInsert
         * @param isColumns
         * @param rowsOrColumns
         */
        void updateRegion(boolean isInsert, boolean isColumns, int rowsOrColumns, IGridRegion r) {
            int inc = isInsert ? rowsOrColumns : -rowsOrColumns;
            if (isColumns) {
                ((GridRegion)r).setRight(r.getRight() + inc);
            } else {
                ((GridRegion)r).setBottom(r.getBottom() + inc);
            }
        }
    }

    /**
     * Table Headers – syntax depends on first keyword
     */
    public static final String TH_CELL_TYPE = "TH_CELL_TYPE";

    /**
     * Condition/Action header
     */
    public static final String CA_HEADER_CELL_TYPE = "CA_HEADER_CELL_TYPE ";

    /**
     * Condition/Action Formula (advanced feature would be the code completion)
     */
    public static final String CA_FORMULA_CELL_TYPE = "CA_FORMULA_CELL_TYPE";

    /**
     * Condition/Action Parameter Definition (type name)
     */
    public static final String CA_PARAMETER_DEFINITION_CELL_TYPE = "CA_PARAMETER_DEFINITION_CELL_TYPE";

    /**
     * Condition/Action Display Column Header
     */
    public static final String CA_DISPLAY_COLUMN_HEADER_CELL_TYPE = "CA_DISPLAY_COLUMN_HEADER_CELL_TYPE";
    /**
     * Condition/Action value cell – the type is defined by Parameter Definition –
     * constrained data entry based on the Domain of the Parameter Type (also
     * known as Subtype in Exigen Rules; the RDF/OWL uses the word Range to
     * define what we used to call Domain, and the word Domain for what we in
     * Java call the Declaring Class or Declaring Type; we need to decide which
     * terminology we are going to use – I start to lean toward RDF/OWL even
     * though it will conflict with our Range classes – will be IntRange,
     * DoubleRange etc. – but the superclass should be renamed to
     * ArithmeticRange); the cell editors should include comboboxes for
     * enumerations, range validators for numbers, other types of specialized
     * editors – calendars, ranges; it also should allow to input formulas –
     * similar to Condition/Action formula if the first character was ‘=’.
     */
    public static final String CA_ENUMERATION_CELL_TYPE = "CA_ENUMERATION_CELL_TYPE";
    public static final String CA_NUMBER_CELL_TYPE = "CA_NUMBER_CELL_TYPE";

    public static final String CA_DATETIME_CELL_TYPE = "CA_DATETIME_CELL_TYPE";

    /**
     * Date Table Column Headers – constrained to the set of the allowed names +
     * continuations (like in address.zip)
     */
    public static final String DT_COLUMN_HEADER_CELL_TYPE = "DT_COLUMN_HEADER_CELL_TYPE";

    /**
     * Data Table Foreign Keys – constrained to the list of tables with the
     * specific type
     */
    public static final String DT_FOREIGN_KEY_CELL_TYPE = "DT_FOREIGN_KEY_CELL_TYPE";

    /**
     * Data Display Column Header
     */
    public static final String DD_COLUMN_HEADER_CELL_TYPE = "DD_COLUMN_HEADER_CELL_TYPE";
    /**
     * Data Cell – the same as for DT cell, except for formulas (for now)
     */
    public static final String DD_FORMULA_CELL_TYPE = "DD_FORMULA_CELL_TYPE";
    public static final String DD_ENUMERATION_CELL_TYPE = "DD_ENUMERATION_CELL_TYPE";
    public static final String DD_NUMBER_CELL_TYPE = "DD_NUMBER_CELL_TYPE";

    public static final String DD_DATETIME_CELL_TYPE = "DD_DATETIME_CELL_TYPE";

    /**
     * Specialty Cells – like Environment include and import cells
     */
    public static final String SPECIAL_CELL_TYPE = "SPECIAL_CELL_TYPE";

    static final boolean COLUMNS = true, ROWS = false, INSERT = true, REMOVE = false;

    private ITable table;
    private IGridTable gridTable;
    private IGridRegion region;
    private String view;
    private int numberOfNonShownRows;
    private int numberOfNonShownCols;
    private boolean showFormulas = false;
    private boolean collapseProps = false;

    private GridTable[] othertables;

    private UndoableActions actions = new UndoableActions();

    private XlsUndoGrid undoGrid;

    FilteredGrid filteredGrid;

    public TableEditorModel(ITable table) {
        this(table, null, false);
    }

    public TableEditorModel(ITable table, String view, boolean showFormulas) {
        this.table = table;
        this.view = view;
        this.gridTable = table.getGridTable(view);
        this.showFormulas = showFormulas;
        region = new GridRegion(getOriginalTable(this.gridTable).getRegion());
        numberOfNonShownRows = gridTable.getRegion().getTop() - region.getTop();
        numberOfNonShownCols = gridTable.getRegion().getLeft() - region.getLeft();
        othertables = new GridSplitter(gridTable.getGrid()).split();

        if (gridTable.getGrid() instanceof XlsSheetGridModel) {
            XlsSheetGridModel grid = (XlsSheetGridModel) gridTable.getGrid();
            undoGrid = new XlsUndoGrid(grid);
        }
        removeThisTable(othertables);
        makeFilteredGrid(gridTable);
    }

    public boolean isBusinessView() {
        return view != null && view.equalsIgnoreCase(IXlsTableNames.VIEW_BUSINESS);
    }
    
    public boolean canAddCols(int nCols) {
        GridRegion testRegion = new GridRegion(region.getTop() - 1, region.getRight() + 1, region.getBottom() + 1,
                region.getRight() + 1 + nCols);
        for (int i = 0; i < othertables.length; i++) {
            if (IGridRegion.Tool.intersects(testRegion, othertables[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean canAddRows(int nRows) {
        GridRegion testRegion = new GridRegion(region.getBottom() + 1, region.getLeft() - 1, region.getBottom() + 1
                + nRows, region.getRight() + 1);
        for (int i = 0; i < othertables.length; i++) {
            if (IGridRegion.Tool.intersects(testRegion, othertables[i])) {
                return false;
            }
        }
        return true;
    }

    public synchronized void cancel() {
        while (actions.hasUndo()) {
            undo();
        }
    }

    public boolean canRemoveCols(int nCols) {
        return IGridRegion.Tool.width(region) > nCols;
    }

    public boolean canRemoveRows(int nRows) {
        return IGridRegion.Tool.height(region) > nRows;
    }

    /**
     * Gets editor metadata for a specified cell
     * 
     * @param row
     * @param column
     * @return editor metadata
     */
    public Object getCellEditorMetadata(int row, int column) {
        return null;
    }

    public CellMetaInfo getCellMetaInfo(int row, int column) {

        CellMetaInfo metaInfo = IWritableGrid.Tool.getCellMetaInfo(gridTable.getGrid(), tX(column), tY(row));

        // System.out.println("Meta:" + metaInfo);
        return metaInfo;

    }

    public ICellStyle getCellStyle(int row, int column) {
        return IWritableGrid.Tool.getCellStyle(gridTable.getGrid(), tX(column), tY(row));
    }

    public ICellInfo getCellInfo(int row, int column) {
        return gridTable.getGrid().getCellInfo(tX(column), tY(row));
    }

    /**
     * After coping or moving the table we need to set new region destination of it
     * @param newRegion New region of the table
     */
    public void setRegion(IGridRegion newRegion) {
        region = newRegion;
    }

    /**
     * Gets type of a specified cell
     * 
     * @param row
     * @param column
     * @return cell type
     */
    public CellType getCellType(int row, int column) {
        // TODO
        switch (column) {
            case 0:
                return CellType.TH_CELL_TYPE;
            case 3:
                return CellType.CA_ENUMERATION_CELL_TYPE;
            default:
                return null;
        }
    }

    public String getCellValue(int row, int column) {
        return gridTable.getGrid().getStringCellValue(tX(column), tY(row));
    }

    private IGridFilter getFilter(int col, int row) {
        FormattedCell fc = filteredGrid.getFormattedCell(region.getLeft() + col, region.getTop() + row);

        if (fc != null) {
            return fc.getFilter();
        }

        return null;
    }

    public void getUndoableActions(TableEditorModel other) {
        actions = other.actions;
        undoGrid = other.undoGrid;
    }

    public ITable getTable() {
        return table;
    }

    /**
     * @return
     */
    public synchronized IGridTable getUpdatedTable() {
        if (isExtendedView()) {
            return new GridTable(region.getTop() - 3, region.getLeft() - 3, region.getBottom() + 3,
                    region.getRight() + 3, gridTable.getGrid());
        }
        return new GridTable(region.getTop() + numberOfNonShownRows, region.getLeft() + numberOfNonShownCols,
                region.getBottom(), region.getRight(), gridTable.getGrid());

    }

    public synchronized IGridTable getUpdatedFullTable() {
        return new GridTable(region, gridTable.getGrid());

    }

    public synchronized boolean hasRedo() {
        return actions.hasRedo();
    }

    public synchronized boolean hasUndo() {
        return actions.hasUndo();
    }

    /**
     * Extracts original table.
     * 
     * @param table Table which we have.
     * @return Original table that includes our table.
     */
    public static IGridTable getOriginalTable(IGridTable table) {
        while (table instanceof AGridTableDelegator) {
            table = ((AGridTableDelegator) table).getOriginalGridTable();
        }
        return table;
    }

    public synchronized void insertColumns(int nCols, int beforeCol) {
        IUndoableGridAction ua = IWritableGrid.Tool.insertColumns(nCols, beforeCol, region, wgrid());
        RegionAction ra = new RegionAction(ua, COLUMNS, INSERT, nCols);
        ra.doSome(region, wgrid(), undoGrid);
        actions.addNewAction(ra);
    }

    public synchronized void insertRows(int nRows, int beforeRow) {
        IUndoableGridAction ua = IWritableGrid.Tool.insertRows(nRows, beforeRow, region, wgrid());
        RegionAction ra = new RegionAction(ua, ROWS, INSERT, nRows);
        ra.doSome(region, wgrid(), undoGrid);
        actions.addNewAction(ra);
    }

    private boolean isExtendedView() {
        // TODO will deal with it later
        if (true) {
            return false;
        }
        return !(canAddRows(1) && canAddCols(1));
    }

    private void makeFilteredGrid(IGridTable gt) {
        IGrid g = gt.getGrid();
        if (g instanceof FilteredGrid) {
            filteredGrid = (FilteredGrid) g;
            return;
        }

        filteredGrid = new FilteredGrid(gt.getGrid(), new IGridFilter[] { new SimpleXlsFormatter() });
    }

    public synchronized void redo() {
        IUndoableAction ua = actions.redo();
        ((RegionAction) ua).doSome(region, wgrid(), undoGrid);
    }

    public synchronized void removeColumns(int nCols, int beforeCol) {
        if (isExtendedView()) {
            beforeCol -= 3;
        }
        if (beforeCol < 0 || beforeCol >= IGridRegion.Tool.width(region)) {
            return;
        }

        IUndoableGridAction ua = IWritableGrid.Tool.removeColumns(nCols, beforeCol, region, wgrid());
        RegionAction ra = new RegionAction(ua, COLUMNS, REMOVE, nCols);
        ra.doSome(region, wgrid(), undoGrid);
        actions.addNewAction(ra);
    }

    public synchronized void removeRows(int nRows, int beforeRow) {
        if (isExtendedView()) {
            beforeRow -= 3;
        }
        if (beforeRow < 0 || beforeRow >= IGridRegion.Tool.height(region)) {
            return;
        }

        IUndoableGridAction ua = IWritableGrid.Tool.removeRows(nRows, beforeRow, region, wgrid());
        RegionAction ra = new RegionAction(ua, ROWS, REMOVE, nRows);
        ra.doSome(region, wgrid(), undoGrid);
        actions.addNewAction(ra);
    }

    /**
     * @param otherTables
     * 
     */
    private synchronized void removeThisTable(GridTable[] otherTables) {
        Vector<GridTable> v = new Vector<GridTable>();
        for (int i = 0; i < otherTables.length; i++) {
            if (!IGridRegion.Tool.intersects(otherTables[i], region)) {
                v.add(otherTables[i]);
            }
        }

        othertables = v.toArray(new GridTable[0]);
    }

    public synchronized void save() throws IOException {
        XlsSheetGridModel xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        xlsgrid.getSheetSource().getWorkbookSource().save();
        actions = new UndoableActions();
    }

    public synchronized void saveAs(String fname) throws IOException {
        XlsSheetGridModel xlsgrid = (XlsSheetGridModel) gridTable.getGrid();
        xlsgrid.getSheetSource().getWorkbookSource().saveAs(fname);
    }

    public synchronized void setCellValue(int row, int col, String value) {
        if (isExtendedView()) {
            row -= 3;
            col -= 3;
        }

        IUndoableGridAction ua = IWritableGrid.Tool.setStringValue(col, row, region, value, getFilter(col, row));
        RegionAction ra = new RegionAction(ua, ROWS, REMOVE, 0);
        ra.doSome(region, wgrid(), undoGrid);
        actions.addNewAction(ra);
    }

    public synchronized void insertProp(String name, String value) {
        IUndoableGridAction ua = IWritableGrid.Tool.insertProp(region, wgrid(), name, value);
        RegionAction ra = new RegionAction(ua, ROWS, INSERT, (ua instanceof AUndoableCellAction) ? 0 : 1);
        ra.doSome(region, wgrid(), undoGrid);
        actions.addNewAction(ra);
    }

    public synchronized void setStyle(int row, int col, ICellStyle style) {
        if (isExtendedView()) {
            row -= 3;
            col -= 3;
        }
        IUndoableGridAction ua = IWritableGrid.Tool.setStyle(col, row, region, style);
        RegionAction ra = new RegionAction(ua, ROWS, REMOVE, 0);
        ra.doSome(region, wgrid(), undoGrid);
        actions.addNewAction(ra);
    }

    public int tX(int col) {
        return region.getLeft() + col;
    }

    public int tY(int row) {
        return region.getTop() + row;
    }

    /**
     * @return Count of rows that is not showed.
     */
    public int getNumberOfNonShownRows() {
        return numberOfNonShownRows;
    }

    /**
     * @return Count of columns that is not showed.
     */
    public int getNumberOfNonShownCols() {
        return numberOfNonShownCols;
    }

    public synchronized void undo() {
        IUndoableAction ua = actions.undo();
        ((RegionAction) ua).undoSome(region, wgrid(), undoGrid);
    }

    /**
     * Checks if cell with row/col coordinates in system of the grid returned by
     * <code>getUpdatedTable</code> methods is inside of table region.
     * 
     * @param row row number in coordinates of <code>getUpdatedTable</code>
     *            grid
     * @param col column number in coordinates of <code>getUpdatedTable</code>
     *            grid
     * @return if cell belongs to the table
     */
    public boolean updatedTableCellInsideTableRegion(int row, int col) {
        if (isExtendedView()) {
            row -= 3;
            col -= 3;
        }
        return (row >= 0 && col >= 0 && row < IGridRegion.Tool.height(region) && col < IGridRegion.Tool.width(region));
    }

    IWritableGrid wgrid() {
        return (IWritableGrid) gridTable.getGrid();
    }

    public boolean isShowFormulas() {
        return showFormulas;
    }

    public void setShowFormulas(boolean showFormulas) {
        this.showFormulas = showFormulas;
    }

    public boolean isCollapseProps() {
        return collapseProps;
    }

    public void setCollapseProps(boolean collapseProps) {
        this.collapseProps = collapseProps;
    }
}