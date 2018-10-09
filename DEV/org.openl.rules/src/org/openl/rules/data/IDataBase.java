/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.util.Collection;

import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.binding.DuplicatedTableException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;

/**
 * @author snshor
 * 
 */
public interface IDataBase {

    ITable registerTable(String tableName, TableSyntaxNode tsn) throws DuplicatedTableException;

    ITable registerNewTable(String tableName, TableSyntaxNode tsn);

    ITable getTable(String name);
    
    void registerTable(ITable newTable) throws DuplicatedTableException;
    
    Collection<ITable> getTables();

    void preLoadTable(ITable table, ITableModel dataModel, ILogicalTable dataWithHeader, OpenlToolAdaptor ota) throws Exception;

}
