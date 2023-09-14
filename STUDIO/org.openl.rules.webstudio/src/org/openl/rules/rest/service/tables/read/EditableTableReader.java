package org.openl.rules.rest.service.tables.read;

import org.openl.rules.rest.model.tables.EditableTableView;
import org.openl.rules.rest.model.tables.TableView;
import org.openl.rules.table.IOpenLTable;

import java.util.function.Supplier;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
public abstract class EditableTableReader<T extends EditableTableView, R extends TableView.Builder<?>> extends TableReader<T, R> {

    public EditableTableReader(Supplier<R> builderCreator) {
        super(builderCreator);
    }

    public abstract boolean supports(IOpenLTable table);
}
