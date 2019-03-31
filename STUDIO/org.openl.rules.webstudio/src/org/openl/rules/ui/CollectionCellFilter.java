package org.openl.rules.ui;

import java.lang.reflect.Array;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.ui.filters.AGridFilter;
import org.openl.rules.webstudio.web.test.Utils;
import org.openl.types.java.JavaOpenClass;

class CollectionCellFilter extends AGridFilter {
    static CollectionCellFilter INSTANCE = new CollectionCellFilter();

    @Override
    public FormattedCell filterFormat(FormattedCell cell) {
        Object cellValue = cell.getObjectValue();
        if (cellValue != null) {
            Class<?> valueType = cellValue.getClass();
            if (valueType.isArray()) {
                String formattedValue;
                if (Array.getLength(cellValue) == 0) {
                    formattedValue = Utils.displayNameForCollection(JavaOpenClass.getOpenClass(valueType), true);
                } else {
                    formattedValue = FormattersManager.format(cellValue);
                }
                cell.setFormattedValue(formattedValue);
            }
        }
        return cell;
    }
}
