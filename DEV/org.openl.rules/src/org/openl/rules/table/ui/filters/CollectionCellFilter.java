package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.OpenClassHelper;

public class CollectionCellFilter extends AGridFilter {

    public FormattedCell filterFormat(FormattedCell cell) {
        Object cellValue = cell.getObjectValue();
        if (cellValue != null) {
            if (cellValue.getClass().isArray()) {
                Object[] array = (Object[]) cellValue;
                String formattedValue = null;
                if (array.length == 0) {
                    formattedValue = OpenClassHelper.displayNameForCollection(JavaOpenClass.getOpenClass(array.getClass()),
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
