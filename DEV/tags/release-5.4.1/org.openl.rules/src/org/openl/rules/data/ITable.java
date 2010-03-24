/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.data.impl.OpenlBasedDataTableModel;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface ITable {

    Object findObject(int columnIndex, String key, IBindingContext cxt) throws BoundError;

    /**
     * @param n
     * @return
     */
    String getColumnDisplay(int n);

    int getColumnIndex(String columnName);

    /**
     * @param n
     * @return
     */
    String getColumnName(int n);

    /**
     * @param n
     * @return
     */
    IOpenClass getColumnType(int n);

    // Object getFirst(Object primaryKey);

    Object getData(int row);

    Object getDataArray();

    IDataTableModel getDataModel();

    /**
     * @return
     */
    IGridTable getHeaderTable();

    String getName();

    /**
     * @return
     */
    int getNumberOfColumns();

    /**
     * @return
     */
    int getNumberOfRows();

    String getPrimaryIndexKey(int row);

    int getRowIndex(Object target);

    /**
     * @param row
     * @return
     */
    IGridTable getRowTable(int row);

    int getSize();

    TableSyntaxNode getTableSyntaxNode();

    Map<String, Integer> getUniqueIndex(int columnIndex) throws BoundError;

    /**
     * @param col
     * @param row
     * @return
     */
    Object getValue(int col, int row);

    Map<String, Integer> makeUniqueIndex(int idx) throws BoundError;

    void populate(IDataBase db, IBindingContext cxt) throws Exception;

    void preLoad(OpenlToolAdaptor ota) throws Exception;

    void setData(ILogicalTable dataWithHeader);

    void setModel(OpenlBasedDataTableModel dataModel);

    void setPrimaryIndexKey(int row, String value);

}
