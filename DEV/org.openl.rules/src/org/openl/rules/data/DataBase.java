/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.DuplicatedTableException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;

/**
 * @author snshor
 *
 */
public class DataBase implements IDataBase {

    private final Map<String, ITable> tables = new HashMap<>();

    private final Object lock = new Object();

    @Override
    public ITable getTable(String name) {
        synchronized (lock) {
            return tables.get(name);
        }
    }

    @Override
    public ITable registerTable(String tableName, TableSyntaxNode tsn) throws DuplicatedTableException {
        synchronized (lock) {
            ITable table = getTable(tableName);

            if (table != null) {
                String uri = table.getTableSyntaxNode().getTable().getSource().getUri();
                String newUri = tsn.getTable().getSource().getUri();
                if (!uri.equals(newUri)) {
                    throw new DuplicatedTableException(tableName, tsn);
                } else {
                    return table;
                }
            }

            table = makeNewTable(tableName, tsn);
            tables.put(tableName, table);

            return table;
        }
    }

    @Override
    public ITable registerNewTable(String tableName, TableSyntaxNode tsn) {
        synchronized (lock) {
            ITable table = makeNewTable(tableName, tsn);
            tables.put(tableName, table);

            return table;
        }
    }

    @Override
    public Set<ITable> getTables() {
        synchronized (lock) {
            return new HashSet<>(this.tables.values());
        }
    }

    @Override
    public void registerTable(ITable newTable) throws DuplicatedTableException {
        synchronized (lock) {
            ITable table = getTable(newTable.getName());
            if (table != null) {
                String uri = table.getUri();
                String newUri = newTable.getUri();
                if (!uri.equals(newUri)) {
                    throw new DuplicatedTableException(newTable.getName(), newTable.getTableSyntaxNode());
                }
            }

            tables.put(newTable.getName(), newTable);
        }
    }

    protected ITable makeNewTable(String tableName, TableSyntaxNode tsn) {
        return new Table(tableName, tsn);
    }

    @Override
    public void preLoadTable(ITable table,
            ITableModel dataModel,
            ILogicalTable dataWithTitles,
            OpenlToolAdaptor openlAdapter) throws Exception {

        table.setModel(dataModel);
        table.setData(dataWithTitles);
        table.preLoad(openlAdapter);
    }

    @Override
    public synchronized void clearOddDataForExecutionMode() {
        Iterator<Map.Entry<String, ITable>> itr = tables.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, ITable> entry = itr.next();
            if (XlsNodeTypes.XLS_DATA.equals(entry.getValue().getXlsNodeType())) {
                entry.getValue().clearOddDataForExecutionMode();
            } else {
                itr.remove();
            }
        }
    }
}
