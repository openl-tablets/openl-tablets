/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.table;

import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 *
 * Table based on Grid coordinates 	
 *
 */
public interface IGridTable extends ILogicalTable
{
	
	public static final boolean 
	      ORIENTATION_NORMAL = true,
	      ORIENTATION_TRANSPOSED = false; 
	
	
	
	
	IGrid getGrid();
		
	int getGridHeight();
	int getGridWidth();

	int getGridColumn(int column, int row);
	int getGridRow(int column, int row);
	
	
	boolean isNormalOrientation();
	
	/**
	 * 
	 * @param col
	 * @param row
	 * @return
	 */
	int getCellHeight(int col, int row);
	
	public boolean isPartOfTheMergedRegion(int column, int row);
	

	int getCellWidth(int col, int row);
	
	String getStringValue(int col, int row);

	String getUri(int col, int row);
	String getUri();

	/**
	 * @param j
	 * @param i
	 * @return
	 */
	ICellStyle getCellStyle(int col, int row);
	
	
	/**
	 * 
	 * @return
	 */
	IGridRegion getRegion();

	ICellInfo getCellInfo(int column, int row);

	/**
	 * @param first
	 * @param rrow
	 * @return
	 */
	Object getObjectValue(int first, int rrow);
	
}
