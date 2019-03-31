package org.openl.rules.ui.copy;

import java.util.List;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class TableNamesCopier extends TableCopier {

    public TableNamesCopier(IOpenLTable table) {
        super(table);
    }

    /*
     * private void validateTechnicalName(TableSyntaxNode node) throws CreateTableException { String[] headerStr =
     * node.getHeaderLineValue().getValue().split(" "); if (headerStr.length >=3) { String existingTechnicalName =
     * headerStr[2].substring(0, headerStr[2].indexOf("(")); if
     * (tableTechnicalName.equalsIgnoreCase(existingTechnicalName)) { throw new
     * CreateTableException("Table with the same technical name already exists"); } } }
     */

    @Override
    public List<TableProperty> getPropertiesToDisplay() {
        return null;
    }

}
