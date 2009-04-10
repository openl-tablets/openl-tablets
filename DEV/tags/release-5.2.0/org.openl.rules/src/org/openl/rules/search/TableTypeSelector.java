/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 */
public class TableTypeSelector extends ATableSyntaxNodeSelector {
    String[] types;

    public TableTypeSelector() {
    }

    public TableTypeSelector(String[] types) {
        this.types = types;
    }

    public String[] getTypes() {
        return types;
    }

    @Override
    public boolean selectTable(TableSyntaxNode node) {
        String type = node.getType();
        return ArrayTool.contains(types, type);
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

}
