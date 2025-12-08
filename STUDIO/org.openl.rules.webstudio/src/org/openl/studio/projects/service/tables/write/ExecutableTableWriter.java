package org.openl.studio.projects.service.tables.write;

import java.util.stream.Collectors;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.ExecutableView;
import org.openl.util.CollectionUtils;

/**
 * Base writer for executable tables
 *
 * @author Vladyslav Pikus
 */
public abstract class ExecutableTableWriter<T extends ExecutableView> extends TableWriter<T> {

    public ExecutableTableWriter(IOpenLTable table) {
        super(table);
    }

    public ExecutableTableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    @Override
    protected void updateHeader(T tableView) {
        var header = new StringBuilder(getBusinessTableType(tableView)).append(' ')
                .append(tableView.returnType)
                .append(' ')
                .append(tableView.name)
                .append('(');
        if (CollectionUtils.isNotEmpty(tableView.args)) {
            var args = tableView.args.stream().map(arg -> arg.type + ' ' + arg.name).collect(Collectors.joining(", "));
            header.append(args);
        }
        header.append(')');
        createOrUpdateCell(getGridTable(), buildCellKey(0, 0), header.toString());
        mergeHeaderCells(tableView);
    }

    protected abstract void mergeHeaderCells(T tableView);
}
