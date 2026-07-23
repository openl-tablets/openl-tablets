package org.openl.rules.ui.copy;

import java.util.List;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class TableNamesCopier extends TableCopier {

    public TableNamesCopier(IOpenLTable table) {
        super(table);
    }

    @Override
    public List<TableProperty> getPropertiesToDisplay() {
        return null;
    }

}
