package org.openl.rules.ui;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.ui.filters.AGridFilter;
import org.openl.rules.webstudio.web.test.Utils;
import org.openl.types.java.JavaOpenClass;

class CollectionCellFilter extends AGridFilter {
    static CollectionCellFilter INSTANCE = new CollectionCellFilter();

    public FormattedCell filterFormat(FormattedCell cell) {
        Object cellValue = cell.getObjectValue();
        if (cellValue != null) {
            if (cellValue.getClass().isArray()) {
                Object[] array = (Object[]) cellValue;
                String formattedValue = null;
                if (array.length == 0) {
                    formattedValue = Utils.displayNameForCollection(JavaOpenClass.getOpenClass(array.getClass()),
                            true);
                } else {
                    formattedValue = FormattersManager.format(array);
                }
                cell.setFormattedValue(formattedValue);
            }
        }
        return cell;
    }
}
