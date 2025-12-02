package org.openl.studio.projects.service.tables.read;

import java.util.function.Supplier;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.TableView;

public abstract class EditableTableReader<T extends TableView, R extends TableView.Builder<?>> extends TableReader<T, R> {

    public EditableTableReader(Supplier<R> builderCreator) {
        super(builderCreator);
    }

    public abstract boolean supports(IOpenLTable table);
}
