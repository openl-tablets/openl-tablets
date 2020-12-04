/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.util.List;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface ITable {

    Object findObject(int columnIndex, String key, IBindingContext bindingContext);

    String getColumnDisplay(int n);

    String getColumnName(int n);

    IOpenClass getColumnType(int n);

    int getColumnIndex(String columnName);

    Object getData(int row);

    Object getDataArray();

    ITableModel getDataModel();

    IGridTable getHeaderTable();

    String getName();

    int getNumberOfColumns();

    ColumnDescriptor getColumnDescriptor(int i);

    int getNumberOfRows();

    String getPrimaryIndexKey(int row);

    Integer getRowIndex(Object target);

    IGridTable getRowTable(int row);

    int getSize();

    TableSyntaxNode getTableSyntaxNode();

    Object getValue(int col, int row);

    Map<String, Integer> makeUniqueIndex(int idx, IBindingContext cxt);

    List<Object> getUniqueValues(int colIdx) throws SyntaxNodeException;

    void populate(IDataBase db, IBindingContext bindingContext) throws Exception;

    void preLoad(OpenlToolAdaptor ota) throws Exception;

    void setData(ILogicalTable dataWithHeader);

    ILogicalTable getData();

    void setModel(ITableModel dataModel);

    void setPrimaryIndexKey(int row, String value);

    void clearOddDataForExecutionMode();

    XlsNodeTypes getXlsNodeType();

    String getUri();
}
