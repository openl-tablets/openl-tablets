/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.binding.DuplicatedTableException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;

/**
 * @author snshor
 * 
 */
public class DataBase implements IDataBase {

    private Map<String, ITable> tables = new HashMap<String, ITable>();

    private final Object lock = new Object();

    public ITable getTable(String name) {
        synchronized (lock) {
            return tables.get(name);
        }
    }

    public ITable registerTable(String tableName, TableSyntaxNode tsn) throws DuplicatedTableException {
        synchronized (lock) {
            ITable table = getTable(tableName);

            if (table != null) {
                String uri = table.getTableSyntaxNode().getTable().getSource().getUri();
                String newUri = tsn.getTable().getSource().getUri();
                if (!uri.equals(newUri)){
                    throw new DuplicatedTableException(tableName, table.getTableSyntaxNode(), tsn);
                }else{
                    return table;    
                }
            }
            
            table = makeNewTable(tableName, tsn);
            tables.put(tableName, table);

            return table;
        }
    }

    public ITable registerNewTable(String tableName, TableSyntaxNode tsn) {
        synchronized (lock) {
            ITable table = makeNewTable(tableName, tsn);
            tables.put(tableName, table);

            return table;
        }
    }
    
    public Set<ITable> getTables(){
        synchronized (lock) {
            Set<ITable> ret = new HashSet<ITable>();
            for (ITable table : this.tables.values()) {
                ret.add(table);
            }
            return ret;
        }
    }
    
    public void registerTable(ITable newTable) throws DuplicatedTableException{
        synchronized (lock) {
            ITable table = getTable(newTable.getName());

            if (table != null) {
                String uri = table.getTableSyntaxNode().getTable().getSource().getUri();
                String newUri = newTable.getTableSyntaxNode().getTable().getSource().getUri();
                if (!uri.equals(newUri)){
                    throw new DuplicatedTableException(newTable.getName(), table.getTableSyntaxNode(), newTable.getTableSyntaxNode());
                }
            }

            tables.put(newTable.getName(), newTable); 
        }       
    }

    protected ITable makeNewTable(String tableName, TableSyntaxNode tsn) {
        return new Table(tableName, tsn);
    }

    public void preLoadTable(ITable table,
            ITableModel dataModel,
            ILogicalTable dataWithTitles,
            OpenlToolAdaptor openlAdapter) throws Exception {

        table.setModel(dataModel);
        table.setData(dataWithTitles);
        table.preLoad(openlAdapter);
    }

}
