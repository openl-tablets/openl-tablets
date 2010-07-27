/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.binding.DuplicatedTableException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;

/**
 * @author snshor
 * 
 */
public interface IDataBase {

    ITable addNewTable(String tableName, TableSyntaxNode tsn) throws DuplicatedTableException;

    ITable getTable(String name);

    void preLoadTable(ITable table, ITableModel dataModel, ILogicalTable dataWithHeader, OpenlToolAdaptor ota) throws Exception;

}
