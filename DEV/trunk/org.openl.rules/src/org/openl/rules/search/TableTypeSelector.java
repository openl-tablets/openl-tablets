/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ArrayTool;

/**
 * Handles array of table types (e.g. rules, spreadsheet, etc. see {@code ITableNodeTypes} constant 
 * for supported types).
 * Checks if given table type exists in current array.
 * @author snshor
 *
 */
public class TableTypeSelector extends ATableSyntaxNodeSelector {
    
    private String[] types;

    public TableTypeSelector() {
    }

    public TableTypeSelector(String[] types) {
        this.types = types;
    }

    public String[] getTypes() {
        return types;
    }
    
    /**
     * Checks if given table type exists in current array.
     */
    @Override
    public boolean doesTableMatch(TableSyntaxNode node) {
        String type = node.getType();
        return ArrayTool.contains(types, type);
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

}
