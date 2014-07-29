package org.openl.rules.table.search.selectors;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Handles array of table types (e.g. rules, spreadsheet, etc. see {@code ITableNodeTypes} constant 
 * for supported types).
 * Checks if given table type exists in current array.
 *
 * @author snshor
 *
 */
public class TableTypeSelector extends TableSelector {

    private String[] types;

    public TableTypeSelector() {
    }

    public TableTypeSelector(String[] types) {
        this.types = types;
    }

    public String[] getTypes() {
        return types;
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

    @Override
    public boolean select(TableSyntaxNode node) {
        String type = node.getType();
        return ArrayUtils.contains(types, type);
    }

}
