/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.util.Collection;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */
public interface ITable {

    /**
     * Finds the row object whose value in the column is the key.
     *
     * <p>Gives {@code null} when no row has the key. A column with no field holds no keys.
     */
    Object findObject(int columnIndex, String key, IBindingContext bindingContext);

    String getColumnName(int n);

    /**
     * Gives the type of the field the column fills.
     *
     * <p>Gives {@code null} for a column with no field and for a column that builds the row object itself.
     */
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

    Map<String, Integer> makeUniqueIndex(int idx, IBindingContext cxt);

    Collection<Object> getUniqueValues(int colIdx) throws SyntaxNodeException;

    void populate(IDataBase db, IBindingContext bindingContext);

    void preLoad(OpenlToolAdaptor ota) throws OpenLCompilationException;

    void setData(ILogicalTable dataWithHeader);

    ILogicalTable getData();

    void setModel(ITableModel dataModel);

    void setPrimaryIndexKey(int row, String value);

    void clearOddDataForExecutionMode();

    XlsNodeTypes getXlsNodeType();

    String getUri();
}
