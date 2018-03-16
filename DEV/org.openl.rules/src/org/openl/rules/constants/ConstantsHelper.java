package org.openl.rules.constants;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.table.ILogicalTable;

class ConstantsHelper {
    private static final int MAXIMUM_COLUMNS_COUNT = 3;

    private static final int DEFAULTS_COLUMN = 0;

    private static final int TYPE_NAME_COLUMN = 0;

    private static final int CONSTANT_NAME_COLUMN = 0;

    private static boolean isThirdColumnForValues(ILogicalTable table) {
        // If first or second row is blank or starts with number, it can't be a type name and field name respectively,
        // in this case we can assume that the third column is definitely for defaults
        return DatatypeHelper.isDefault(table.getCell(DEFAULTS_COLUMN, TYPE_NAME_COLUMN)) ||
                DatatypeHelper.isDefault(table.getCell(DEFAULTS_COLUMN, CONSTANT_NAME_COLUMN));
    }

    public static ILogicalTable getNormalizedDataPartTable(ILogicalTable table, OpenL openl, IBindingContext cxt) {
        ILogicalTable dataPart = table.getRows(1);

        if (dataPart == null) {
            return null;
        }

        // if constants table has only one row
        if (dataPart.getHeight() == 1) {
            return dataPart;
        } else if (dataPart.getWidth() == 1) {
            return dataPart.transpose();
        }

        if (dataPart.getHeight() > MAXIMUM_COLUMNS_COUNT) {
            return dataPart;
        }

        if (dataPart.getWidth() > MAXIMUM_COLUMNS_COUNT) {
            return dataPart.transpose();
        }

        if (dataPart.getWidth() == MAXIMUM_COLUMNS_COUNT && isThirdColumnForValues(dataPart)) {
            return dataPart;
        }

        if (dataPart.getHeight() == MAXIMUM_COLUMNS_COUNT && isThirdColumnForValues(dataPart.transpose())) {
            return dataPart.transpose();
        }

        int verticalCount = DatatypeHelper.countTypes(dataPart, openl, cxt);
        if (verticalCount == dataPart.getHeight() && verticalCount >= dataPart.getWidth()) {
            // There is no need to check horizontal types.
            return dataPart;
        }
        int horizontalCount = DatatypeHelper.countTypes(dataPart.transpose(), openl, cxt);

        if (verticalCount < horizontalCount) {
            return dataPart.transpose();
        }

        return dataPart;
    }
}
