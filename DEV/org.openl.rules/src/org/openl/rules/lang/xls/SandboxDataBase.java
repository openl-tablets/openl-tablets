package org.openl.rules.lang.xls;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.data.DataBase;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.data.ITableModel;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.TableProperties;

/**
 * The database of a table bound alone against a compiled module.
 *
 * <p>A table it registers is new, and stays in this database: the tables of the module are left as compiled. A table
 * it does not hold is found among the tables of the module.
 *
 * <p>The structure of a table is read, its rows are not: a Data, a Test or a Run table takes its columns and their
 * types, with no value converted. The properties a table declares are read in full, since they tell how it is bound.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SandboxDataBase extends DataBase {

    private final IDataBase compiled;

    @Override
    public ITable registerTable(String tableName, TableSyntaxNode tsn) {
        return registerNewTable(tableName, tsn);
    }

    @Override
    public @Nullable ITable getTable(String name) {
        var table = super.getTable(name);
        return table != null ? table : compiled.getTable(name);
    }

    @Override
    public void preLoadTable(ITable table,
                             ITableModel dataModel,
                             ILogicalTable dataWithTitles,
                             OpenlToolAdaptor openlAdapter) throws OpenLCompilationException {
        if (TableProperties.class == dataModel.getInstanceClass()) {
            super.preLoadTable(table, dataModel, dataWithTitles, openlAdapter);
        } else {
            table.setModel(dataModel);
            table.setData(dataWithTitles);
        }
    }
}
