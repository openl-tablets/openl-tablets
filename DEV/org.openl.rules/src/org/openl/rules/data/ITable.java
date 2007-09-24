/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.data;

import org.openl.binding.IBindingContext;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface ITable
{

	Object getDataArray();
	Object getData(int row);
	int getSize();	
	Object getFirst(Object primaryKey);

	IDataTableModel getDataModel();

	int getColumnIndex(String columnName);
	Object findObject(int columnIndex, String key, IBindingContext cxt);

	void populate(IDataBase db, IBindingContext cxt) throws Exception;
	/**
	 * @return
	 */
	int getNumberOfRows();
	/**
	 * @return
	 */
	int getNumberOfColumns();
	/**
	 * @param n
	 * @return
	 */
	String getColumnName(int n);
	/**
	 * @param n
	 * @return
	 */
	String getColumnDisplay(int n);
	/**
	 * @param n
	 * @return
	 */
	IOpenClass getColumnType(int n);
	/**
	 * @param col
	 * @param row
	 * @return
	 */
	Object getValue(int col, int row);
	/**
	 * @param row
	 * @return
	 */
	IGridTable getRowTable(int row);
	/**
	 * @return
	 */
	IGridTable getHeaderTable();
	
	
}
