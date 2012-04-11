/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.rules.ui;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IUndoGrid;
import org.openl.rules.table.IUndoableAction;
import org.openl.rules.table.IUndoableGridAction;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.UndoableActions;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.XlsUndoGrid;

import java.io.IOException;
import java.util.Vector;

/**
 * @author snshor
 * 
 */
public class TableEditorModel
{
    public static enum CellType
    {
	TH_CELL_TYPE, CA_HEADER_CELL_TYPE, CA_ENUMERATION_CELL_TYPE
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

    static final boolean COLUMNS = true, ROWS = false, INSERT = true,
	    REMOVE = false;

    private IGridTable table;

    private GridRegion region;
    private GridTable[] othertables;

    private UndoableActions actions = new UndoableActions();

    private XlsUndoGrid undoGrid = new XlsUndoGrid();

    public TableEditorModel(IGridTable table)
    {
	this.table = table;
	this.region = new GridRegion(table.getRegion());
	othertables = new GridSplitter(table.getGrid()).split();
	removeThisTable(othertables);
    }

    /**
     * @param otherTables
     * 
     */
    private synchronized void removeThisTable(GridTable[] otherTables)
    {
	Vector<GridTable> v = new Vector<GridTable>();
	for (int i = 0; i < otherTables.length; i++)
	{
	    if (!IGridRegion.Tool.intersects(otherTables[i], region))
	    {
		v.add(otherTables[i]);
	    }
	}

	this.othertables = v.toArray(new GridTable[0]);
    }

    public void getUndoableActions(TableEditorModel other)
    {
	actions = other.actions;
	undoGrid = other.undoGrid;
    }

    IWritableGrid wgrid()
    {
	return (IWritableGrid) table.getGrid();
    }

    public synchronized void insertRows(int nRows, int beforeRow)
    {
	IUndoableGridAction ua = IWritableGrid.Tool.insertRows(nRows,
		beforeRow, region, wgrid());
	RegionAction ra = new RegionAction(ua, ROWS, INSERT, nRows);
	ra.doSome(region, wgrid(), undoGrid);
	actions.addNewAction(ra);
    }

    public synchronized void insertColumns(int nCols, int beforeCol)
    {
	IUndoableGridAction ua = IWritableGrid.Tool.insertColumns(nCols,
		beforeCol, region, wgrid());
	RegionAction ra = new RegionAction(ua, COLUMNS, INSERT, nCols);
	ra.doSome(region, wgrid(), undoGrid);
	actions.addNewAction(ra);
    }

    public synchronized void removeRows(int nRows, int beforeRow)
    {
	if (isExtendedView())
	    beforeRow -= 3;
	if (beforeRow < 0 || beforeRow >= IGridRegion.Tool.height(region))
	    return;

	IUndoableGridAction ua = IWritableGrid.Tool.removeRows(nRows,
		beforeRow, region);
	RegionAction ra = new RegionAction(ua, ROWS, REMOVE, nRows);
	ra.doSome(region, wgrid(), undoGrid);
	actions.addNewAction(ra);
    }

    public synchronized void removeColumns(int nCols, int beforeCol)
    {
	if (isExtendedView())
	    beforeCol -= 3;
	if (beforeCol < 0 || beforeCol >= IGridRegion.Tool.width(region))
	    return;

	IUndoableGridAction ua = IWritableGrid.Tool.removeColumns(nCols,
		beforeCol, region);
	RegionAction ra = new RegionAction(ua, COLUMNS, REMOVE, nCols);
	ra.doSome(region, wgrid(), undoGrid);
	actions.addNewAction(ra);
    }

    public synchronized void setCellValue(int row, int col, String value)
    {
	if (isExtendedView())
	{
	    row -= 3;
	    col -= 3;
	}
	IUndoableGridAction ua = IWritableGrid.Tool.setStringValue(col, row,
		region, value);
	RegionAction ra = new RegionAction(ua, ROWS, REMOVE, 0);
	ra.doSome(region, wgrid(), undoGrid);
	actions.addNewAction(ra);
    }

    public synchronized void setStyle(int row, int col, ICellStyle style)
    {
	if (isExtendedView())
	{
	    row -= 3;
	    col -= 3;
	}
	IUndoableGridAction ua = IWritableGrid.Tool.setStyle(col, row, region,
		style);
	RegionAction ra = new RegionAction(ua, ROWS, REMOVE, 0);
	ra.doSome(region, wgrid(), undoGrid);
	actions.addNewAction(ra);
    }

    public ICellStyle getCellStyle(int row, int column)
    {
	return IWritableGrid.Tool.getCellStyle(table.getGrid(), tX(column),
		tY(row));
    }

    public String getCellValue(int row, int column)
    {
	return table.getGrid().getStringCellValue(tX(column), tY(row));
    }

    static class RegionAction implements IUndoableAction
    {
	IUndoableGridAction gridAction;

	boolean isInsert;
	boolean isColumns;
	int nRowsOrColumns;

	public RegionAction(IUndoableGridAction action, boolean isColumns,
		boolean isInsert, int nRowsOrColumns)
	{
	    this.gridAction = action;
	    this.isColumns = isColumns;
	    this.isInsert = isInsert;
	    this.nRowsOrColumns = nRowsOrColumns;
	}

	public void doSome(GridRegion r, IWritableGrid wgrid, IUndoGrid undoGrid)
	{
	    gridAction.doAction(wgrid, undoGrid);
	    updateRegion(isInsert, isColumns, nRowsOrColumns, r);
	}

	/**
	 * @param isInsert2
	 * @param isColumns2
	 * @param rowsOrColumns
	 */
	void updateRegion(boolean isInsert, boolean isColumns,
		int rowsOrColumns, GridRegion r)
	{
	    int inc = isInsert ? rowsOrColumns : -rowsOrColumns;
	    if (isColumns)
		r.setRight(r.getRight() + inc);
	    else
		r.setBottom(r.getBottom() + inc);
	}

	public void undoSome(GridRegion r, IWritableGrid wgrid,
		IUndoGrid undoGrid)
	{
	    updateRegion(!isInsert, isColumns, nRowsOrColumns, r);
	    gridAction.undoAction(wgrid, undoGrid);
	}
    }

    public synchronized boolean hasUndo()
    {
	return actions.hasUndo();
    }

    public synchronized boolean hasRedo()
    {
	return actions.hasRedo();
    }

    public synchronized void undo()
    {
	IUndoableAction ua = actions.undo();
	((RegionAction) ua).undoSome(region, wgrid(), undoGrid);
    }

    public synchronized void redo()
    {
	IUndoableAction ua = actions.redo();
	((RegionAction) ua).doSome(region, wgrid(), undoGrid);
    }

    public synchronized void cancel()
    {
	while (actions.hasUndo())
	    undo();
    }

    public synchronized void save() throws IOException
    {
	XlsSheetGridModel xlsgrid = (XlsSheetGridModel) table.getGrid();
	xlsgrid.getSheetSource().getWorkbookSource().save();
	actions = new UndoableActions();
    }

    public synchronized void saveAs(String fname) throws IOException
    {
	XlsSheetGridModel xlsgrid = (XlsSheetGridModel) table.getGrid();
	xlsgrid.getSheetSource().getWorkbookSource().saveAs(fname);
    }

    /**
     * @return
     */
    public synchronized IGridTable getUpdatedTable()
    {
	if (isExtendedView())
	{
	    return new GridTable(region.getTop() - 3, region.getLeft() - 3,
		    region.getBottom() + 3, region.getRight() + 3, table
			    .getGrid());
	}
	return new GridTable(region.getTop(), region.getLeft(), region
		.getBottom(), region.getRight(), table.getGrid());

    }

    public int tX(int col)
    {
	return region.getLeft() + col;
    }

    public int tY(int row)
    {
	return region.getTop() + row;
    }

    private boolean isExtendedView()
    {
	// TODO will deal with it later
	if (true)
	    return false;
	return !(canAddRows(1) && canAddCols(1));
    }

    /**
     * Checks if cell with row/col coordinates in system of the grid returned by
     * <code>getUpdatedTable</code> methods is inside of table region.
     * 
     * @param row
     *                row number in coordinates of <code>getUpdatedTable</code>
     *                grid
     * @param col
     *                column number in coordinates of
     *                <code>getUpdatedTable</code> grid
     * @return if cell belongs to the table
     */
    public boolean updatedTableCellInsideTableRegion(int row, int col)
    {
	if (isExtendedView())
	{
	    row -= 3;
	    col -= 3;
	}
	return (row >= 0 && col >= 0 && row < IGridRegion.Tool.height(region) && col < IGridRegion.Tool
		.width(region));
    }

    public boolean canAddRows(int nRows)
    {
	GridRegion testRegion = new GridRegion(region.getBottom() + 1, region
		.getLeft() - 1, region.getBottom() + 1 + nRows, region
		.getRight() + 1);
	for (int i = 0; i < othertables.length; i++)
	{
	    if (IGridRegion.Tool.intersects(testRegion, othertables[i]))
		return false;
	}
	return true;
    }

    public boolean canRemoveRows(int nRows)
    {
	return IGridRegion.Tool.height(region) > nRows;
    }

    public boolean canAddCols(int nCols)
    {
	GridRegion testRegion = new GridRegion(region.getTop() - 1, region
		.getRight() + 1, region.getBottom() + 1, region.getRight() + 1
		+ nCols);
	for (int i = 0; i < othertables.length; i++)
	{
	    if (IGridRegion.Tool.intersects(testRegion, othertables[i]))
		return false;
	}
	return true;
    }

    public boolean canRemoveCols(int nCols)
    {
	return IGridRegion.Tool.width(region) > nCols;
    }

    /**
     * Gets type of a specified cell
     * 
     * @param row
     * @param column
     * @return cell type
     */
    public CellType getCellType(int row, int column)
    {
	// TODO
	switch (column)
	{
	case 0:
	    return CellType.TH_CELL_TYPE;
	case 3:
	    return CellType.CA_ENUMERATION_CELL_TYPE;
	default:
	    return null;
	}
    }

    public CellMetaInfo getCellMetaInfo(int row, int column)
    {

	CellMetaInfo metaInfo = IWritableGrid.Tool.getCellMetaInfo(table
		.getGrid(), tX(column), tY(row));

	// System.out.println("Meta:" + metaInfo);
	return metaInfo;

    }

    /**
     * Gets editor metadata for a specified cell
     * 
     * @param row
     * @param column
     * @return editor metadata
     */
    public Object getCellEditorMetadata(int row, int column)
    {

	CellMetaInfo metaInfo = IWritableGrid.Tool.getCellMetaInfo(table
		.getGrid(), tX(column), tY(row));

	System.out.println("Meta:" + metaInfo);

	// TODO
	switch (column)
	{
	case 3:
	    return new String[] { "ALABAMA", "ALASKA", "AMERICAN SAMOA",
		    "ARIZONA", "ARKANSAS", "CALIFORNIA", "COLORADO",
		    "CONNECTICUT", "DELAWARE", "DISTRICT OF COLUMBIA",
		    "FEDERATED STATES OF MICRONESIA", "FLORIDA", "GEORGIA",
		    "GUAM", "HAWAII", "IDAHO", "ILLINOIS", "INDIANA", "IOWA",
		    "KANSAS", "KENTUCKY", "LOUISIANA", "MAINE",
		    "MARSHALL ISLANDS", "MARYLAND", "MASSACHUSETTS",
		    "MICHIGAN", "MINNESOTA", "MISSISSIPPI", "MISSOURI",
		    "MONTANA", "NEBRASKA", "NEVADA", "NEW HAMPSHIRE",
		    "NEW JERSEY", "NEW MEXICO", "NEW YORK", "NORTH CAROLINA",
		    "NORTH DAKOTA", "NORTHERN MARIANA ISLANDS", "OHIO",
		    "OKLAHOMA", "OREGON", "PALAU", "PENNSYLVANIA",
		    "PUERTO RICO", "RHODE ISLAND", "SOUTH CAROLINA",
		    "SOUTH DAKOTA", "TENNESSEE", "TEXAS", "UTAH", "VERMONT",
		    "VIRGIN ISLANDS", "VIRGINIA", "WASHINGTON",
		    "WEST VIRGINIA", "WISCONSIN", "WYOMING" };
	default:
	    return null;
	}
    }
}