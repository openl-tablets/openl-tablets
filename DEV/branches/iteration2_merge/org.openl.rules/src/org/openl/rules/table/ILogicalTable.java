/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.table;

/**
 * @author snshor
 * 
 * Logical Table may consist of logical columns and rows (created as a result of merged cells). 
 *
 */
public interface ILogicalTable
{
	/**
	 * @return underlying grid table
	 */

	IGridTable getGridTable();

	int getLogicalHeight();
	int getLogicalWidth();


	ILogicalTable getLogicalRow(int row);
	int getLogicalRowGridHeight(int row);
	int getLogicalColumnGridWidth(int column);
	
	ILogicalTable getLogicalColumn(int column);
	ILogicalTable getLogicalRegion(int column, int row, int width, int height);

	
	ILogicalTable rows(int from, int to);
	ILogicalTable columns(int from, int to);

	ILogicalTable rows(int from);
	ILogicalTable columns(int from);
	
	/**
	 * Calculates # of the column starting exactly at gridOffset. 
	 * Throws TableException if gridOffset does not match any column's start  
	 * @param gridOffset
	 * @return
	 */
	int findColumnStart(int gridOffset) throws TableException;
	
	/**
	 * Calculates # of the row starting exactly at gridOffset. 
	 * Throws TableException if gridOffset does not match any row's start  
	 * @param gridOffset
	 * @return
	 */
	int findRowStart(int gridOffset) throws TableException;
	
	
	ILogicalTable transpose();

}
