package org.openl.rules.datatype.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.util.StringUtils;

public class DatatypeHelper {
    /**
     * Datatype table can contain no more than 3 columns: 1) First column - type name 2) Second column - field name 3)
     * Third column - default value
     */
    private static final int MAXIMUM_COLUMNS_COUNT = 3;
    private static final int TYPE_NAME_COLUMN = 0;
    private static final int FIELD_NAME_COLUMN = 1;
    private static final int DEFAULTS_COLUMN = 2;

    public static ILogicalTable getNormalizedDataPartTable(ILogicalTable table, OpenL openl, IBindingContext cxt) {

        ILogicalTable dataPart;
        if (PropertiesHelper.getPropertiesTableSection(table) != null) {
            dataPart = table.getRows(2);
        } else {
            dataPart = table.getRows(1);
        }

        if (dataPart == null) {
            return null;
        }

        // if datatype table has only one row
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

        if (dataPart.getWidth() == MAXIMUM_COLUMNS_COUNT && isThirdColumnForDefaults(dataPart)) {
            return dataPart;
        }

        if (dataPart.getHeight() == MAXIMUM_COLUMNS_COUNT && isThirdColumnForDefaults(dataPart.transpose())) {
            return dataPart.transpose();
        }

        int verticalCount = countTypes(dataPart, openl, cxt);
        if (verticalCount == dataPart.getHeight() && verticalCount >= dataPart.getWidth()) {
            // There is no need to check horizontal types.
            return dataPart;
        }
        int horizontalCount = countTypes(dataPart.transpose(), openl, cxt);

        if (verticalCount < horizontalCount) {
            return dataPart.transpose();
        }

        return dataPart;
    }

    private static boolean isThirdColumnForDefaults(ILogicalTable table) {
        // If first or second row is blank or starts with number, it cannot be a type name and field name respectively,
        // in this case we can assume that the third column is definitely for defaults
        return isDefault(table.getCell(DEFAULTS_COLUMN, TYPE_NAME_COLUMN)) || isDefault(
            table.getCell(DEFAULTS_COLUMN, FIELD_NAME_COLUMN));
    }

    private static boolean isDefault(ICell cell) {
        // Type name and field name cannot be blank or start with number but default value can.
        String value = cell.getStringValue();
        if (StringUtils.isBlank(value)) {
            return true;
        }

        char firstChar = value.charAt(0);
        return '0' <= firstChar && firstChar <= '9';

    }

    private static int countTypes(ILogicalTable table, OpenL openl, IBindingContext cxt) {

        int height = table.getHeight();
        int count = 1; // The first cell is always type name, there is no need to check it. Start from the second one.

        cxt.pushErrors();
        for (int i = 1; i < height; ++i) {
            ILogicalTable row = table.getRow(i);
            GridCellSourceCodeModule source = new GridCellSourceCodeModule(row.getSource(), cxt);
            String code = row.getCell(0, 0).getStringValue();
            if (StringUtils.isBlank(code)) {
                continue;
            }
            IOpenClass type = RuleRowHelper.getType(code, source, cxt);
            if (type != NullOpenClass.the) {
                count += 1;
            }
        }
        cxt.popErrors();

        return count;
    }
}
