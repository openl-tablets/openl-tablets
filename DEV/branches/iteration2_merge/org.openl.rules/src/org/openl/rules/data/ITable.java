/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.data;

import java.util.Map;

import org.openl.OpenlToolAdaptor;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.rules.data.impl.OpenlBasedDataTableModel;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface ITable
{
	
	String getName();
	TableSyntaxNode getTableSyntaxNode();

	Object getDataArray();
	Object getData(int row);
	int getSize();	
//	Object getFirst(Object primaryKey);

	IDataTableModel getDataModel();

	int getColumnIndex(String columnName);
	Object findObject(int columnIndex, String key, IBindingContext cxt) throws BoundError;

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
	
	
	Map<String, Integer> makeUniqueIndex(int idx) throws BoundError;
	public Map<String, Integer> getUniqueIndex(int columnIndex) throws BoundError;
	void setModel(OpenlBasedDataTableModel dataModel);
	void setData(ILogicalTable dataWithHeader);
	void preLoad(OpenlToolAdaptor ota)throws Exception ;
	int getRowIndex(Object target);
	String getPrimaryIndexKey(int row);
	void setPrimaryIndexKey(int row, String value);
	
	
}
