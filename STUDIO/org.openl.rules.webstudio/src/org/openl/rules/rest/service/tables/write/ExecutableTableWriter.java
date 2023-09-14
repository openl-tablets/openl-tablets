package org.openl.rules.rest.service.tables.write;

import org.openl.rules.rest.model.tables.ExecutableView;
import org.openl.rules.table.IOpenLTable;
import org.openl.util.CollectionUtils;

import java.util.stream.Collectors;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
public abstract class ExecutableTableWriter<T extends ExecutableView> extends TableWriter<T> {

    public ExecutableTableWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void updateHeader(T tableView) {
        var header = new StringBuilder(getBusinessTableType()).append(' ')
                .append(tableView.returnType)
                .append(' ')
                .append(tableView.name)
                .append('(');
        if (CollectionUtils.isNotEmpty(tableView.args)) {
            var args = tableView.args.stream().map(arg -> arg.type + ' ' + arg.name).collect(Collectors.joining(", "));
            header.append(args);
        }
        header.append(')');
        createOrUpdateCell(table.getGridTable(), buildCellKey(0, 0), header.toString());
    }
}
