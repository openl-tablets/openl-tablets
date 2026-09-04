package org.openl.rules.datatype.binding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

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

        var verticalTitles = 0;
        var horizontalTitles = 0;
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

        var verticalCount = countTypes(dataPart, cxt);
        if (verticalCount == dataPart.getHeight() && verticalCount >= dataPart.getWidth()) {
            // There is no need to check horizontal types.
            return dataPart;
        }
        var horizontalCount = countTypes(dataPart.transpose(), cxt);

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
        var value = cell.getStringValue();
        if (StringUtils.isBlank(value)) {
            return true;
        }

        var firstChar = value.charAt(0);
        return '0' <= firstChar && firstChar <= '9';

    }

    private static int countTypes(ILogicalTable table, IBindingContext cxt) {

        var height = table.getHeight();
        var count = 1; // The first cell is always type name, there is no need to check it. Start from the second one.

        cxt.pushErrors();
        try {
            for (var i = 1; i < height; ++i) {
                var row = table.getRow(i);
                var source = new GridCellSourceCodeModule(row.getSource(), cxt);
                var code = row.getCell(0, 0).getStringValue();
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

    /**
     * Whether the data part opens with a row of column titles rather than with a field.
     *
     * <p>A datatype declares its columns by naming them, and it does so only when the first row names both
     * {@code Type} and {@code Name} — anything else is the legacy positional layout, whose first row is already
     * a field. Everything that reads a datatype asks this, so they agree on where the fields start.
     *
     * <p>The answer is about the table as it is given. A caller that also normalizes orientation, as the binder
     * does, passes the upright table; one that reads a table as written passes it as written.
     *
     * @param dataPart the body of the table below its header, normalized
     * @return {@code true} when the first row titles the columns
     */
    public static boolean hasColumnTitles(ILogicalTable dataPart) {
        return dataPart != null && hasColumnTitles(dataPart.getWidth(), cellTextOf(dataPart));
    }

    /**
     * The same rule, for a caller that reads a cell's text its own way.
     *
     * <p>The binder reads it as a source code module, so that a cell OpenL cannot parse is reported where it
     * sits; the editor reads the plain value. What counts as a title row must not depend on which of the two
     * is asking.
     *
     * @param width     number of columns in the data part
     * @param titleAt   the text of the first row's cell at the given column
     */
    public static boolean hasColumnTitles(int width, IntFunction<String> titleAt) {
        var hasType = false;
        var hasName = false;
        for (var i = 0; i < width; i++) {
            var title = titleAt.apply(i);
            hasType = hasType || TYPE_COLUMN_TITLE.equals(title);
            hasName = hasName || NAME_COLUMN_TITLE.equals(title);
        }
        return hasType && hasName;
    }

    private static IntFunction<String> cellTextOf(ILogicalTable dataPart) {
        if (dataPart.getHeight() == 0) {
            return column -> null;
        }
        var row = dataPart.getRow(0);
        return column -> row.getCell(column, 0).getStringValue();
    }

    /**
     * Where each column the data part names sits, or no entry at all when it names none.
     *
     * <p>A titled body may write its columns in any order, so a reader takes the position from the title rather
     * than from the column's place.
     *
     * @param dataPart the body of the table below its header, normalized
     * @return the index of every recognized title, empty for the legacy positional layout
     */
    public static Map<String, Integer> getColumnTitlesOrder(ILogicalTable dataPart) {
        return hasColumnTitles(dataPart) ? titlesOfFirstRow(dataPart) : Map.of();
    }

    private static Map<String, Integer> titlesOfFirstRow(ILogicalTable dataPart) {
        var titles = new LinkedHashMap<String, Integer>();
        var row = dataPart.getRow(0);
        for (var i = 0; i < dataPart.getWidth(); i++) {
            var code = row.getCell(i, 0).getStringValue();
            // A title row may leave a cell blank, and the immutable list refuses to be asked about null.
            if (code != null && COLUMN_TITLES.contains(code)) {
                titles.putIfAbsent(code, i);
            }
        }
        return titles;
    }

    private static int countTitles(ILogicalTable table) {
        var width = table.getWidth();
        var count = 0; // The first cell is always title, there is no need to check it. Start from the second one.
        var row = table.getRow(0);
        for (var i = 1; i < width; i++) {
            var code = row.getCell(i, 0).getStringValue();

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
