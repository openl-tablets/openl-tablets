package org.openl.rules.datatype.binding;

import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.engine.OpenLManager;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.util.StringUtils;

public class DatatypeHelper {
    public static final String TYPE_COLUMN_TITLE = "Type";
    public static final String NAME_COLUMN_TITLE = "Name";
    public static final String DEFAULT_COLUMN_TITLE = "Default";
    public static final String EXAMPLE_COLUMN_TITLE = "Example";
    public static final String DESCRIPTION_COLUMN_TITLE = "Description";
    public static final String MANDATORY_COLUMN_TITLE = "Mandatory";

    public static final List<String> COLUMN_TITLES = List.of(TYPE_COLUMN_TITLE, NAME_COLUMN_TITLE, DEFAULT_COLUMN_TITLE, EXAMPLE_COLUMN_TITLE, DESCRIPTION_COLUMN_TITLE, MANDATORY_COLUMN_TITLE);

    /**
     * Datatype table can contain no more than 7 columns: 1) First column - type name 2) Second column - field name 3)
     * Third column - default value, if the width of the table is 3. If the width of the table is more than 3, the table must contain titles for each column.
     */
    private static final int MAXIMUM_COLUMNS_COUNT_NO_TITLES = 3;
    private static final int MAXIMUM_COLUMNS_COUNT = COLUMN_TITLES.size();
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

        if (dataPart.getWidth() == MAXIMUM_COLUMNS_COUNT_NO_TITLES && isThirdColumnForDefaults(dataPart)) {
            return dataPart;
        }

        if (dataPart.getHeight() == MAXIMUM_COLUMNS_COUNT_NO_TITLES && isThirdColumnForDefaults(dataPart.transpose())) {
            return dataPart.transpose();
        }

        int verticalTitles = 0;
        int horizontalTitles = 0;
        if (dataPart.getWidth() > MAXIMUM_COLUMNS_COUNT_NO_TITLES) {
            verticalTitles = countTitles(dataPart);
        }
        if (dataPart.getHeight() > MAXIMUM_COLUMNS_COUNT_NO_TITLES) {
            horizontalTitles = countTitles(dataPart.transpose());
        }

        if (verticalTitles > horizontalTitles && verticalTitles > 0) {
            return dataPart;
        } else if (horizontalTitles > verticalTitles && horizontalTitles > 0) {
            return dataPart.transpose();
        }

        int verticalCount = countTypes(dataPart, cxt);
        if (verticalCount == dataPart.getHeight() && verticalCount >= dataPart.getWidth()) {
            // There is no need to check horizontal types.
            return dataPart;
        }
        int horizontalCount = countTypes(dataPart.transpose(), cxt);

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

    private static int countTypes(ILogicalTable table, IBindingContext cxt) {

        int height = table.getHeight();
        int count = 1; // The first cell is always type name, there is no need to check it. Start from the second one.

        cxt.pushErrors();
        try {
            for (int i = 1; i < height; ++i) {
                ILogicalTable row = table.getRow(i);
                GridCellSourceCodeModule source = new GridCellSourceCodeModule(row.getSource(), cxt);
                String code = row.getCell(0, 0).getStringValue();
                if (StringUtils.isBlank(code)) {
                    continue;
                }
                IOpenClass type = OpenLManager.makeType(cxt.getOpenL(), code, source, cxt);
                if (type != NullOpenClass.the) {
                    count += 1;
                }
            }
        } finally {
            cxt.popErrors();
        }
        return count;
    }

    private static int countTitles(ILogicalTable table) {
        int width = table.getWidth();
        int count = 0; // The first cell is always title, there is no need to check it. Start from the second one.
        ILogicalTable row = table.getRow(0);
        for (int i = 1; i < width; i++) {
            String code = row.getCell(i, 0).getStringValue();

            if (StringUtils.isBlank(code)) {
                continue;
            }

            for (String title : COLUMN_TITLES) {
                if (title.equals(code)) {
                    count += 1;
                    break;
                }
            }
        }

        return count;
    }
}
