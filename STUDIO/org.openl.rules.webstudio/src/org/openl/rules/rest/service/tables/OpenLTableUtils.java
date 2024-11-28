package org.openl.rules.rest.service.tables;

import java.util.Objects;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.rest.model.tables.SimpleRulesView;
import org.openl.rules.rest.model.tables.SmartRulesView;
import org.openl.rules.rest.service.tables.write.VocabularyTableWriter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ITable;
import org.openl.util.StringUtils;

/**
 * Utility class for rule tables.
 */
public abstract class OpenLTableUtils {

    private OpenLTableUtils() {
    }

    private static final BiMap<String, String> TABLE_TYPE_ITEMS;

    static {
        BiMap<String, String> tableTypeItems = HashBiMap.create();
        tableTypeItems.put(XlsNodeTypes.XLS_DT.toString(), "Rules");
        tableTypeItems.put(XlsNodeTypes.XLS_SPREADSHEET.toString(), "Spreadsheet");
        tableTypeItems.put(XlsNodeTypes.XLS_TBASIC.toString(), "TBasic");
        tableTypeItems.put(XlsNodeTypes.XLS_COLUMN_MATCH.toString(), "Column Match");
        tableTypeItems.put(XlsNodeTypes.XLS_DATATYPE.toString(), "Datatype");
        tableTypeItems.put(XlsNodeTypes.XLS_DATA.toString(), "Data");
        tableTypeItems.put(XlsNodeTypes.XLS_METHOD.toString(), "Method");
        tableTypeItems.put(XlsNodeTypes.XLS_TEST_METHOD.toString(), "Test");
        tableTypeItems.put(XlsNodeTypes.XLS_RUN_METHOD.toString(), "Run");
        tableTypeItems.put(XlsNodeTypes.XLS_CONSTANTS.toString(), "Constants");
        tableTypeItems.put(XlsNodeTypes.XLS_CONDITIONS.toString(), "Conditions");
        tableTypeItems.put(XlsNodeTypes.XLS_ACTIONS.toString(), "Actions");
        tableTypeItems.put(XlsNodeTypes.XLS_RETURNS.toString(), "Returns");
        tableTypeItems.put(XlsNodeTypes.XLS_ENVIRONMENT.toString(), "Environment");
        tableTypeItems.put(XlsNodeTypes.XLS_PROPERTIES.toString(), "Properties");
        tableTypeItems.put(XlsNodeTypes.XLS_OTHER.toString(), "Other");

        TABLE_TYPE_ITEMS = Maps.unmodifiableBiMap(tableTypeItems);
    }

    public static BiMap<String, String> getTableTypeItems() {
        return TABLE_TYPE_ITEMS;
    }

    /**
     * Checks if provided table is a vocabulary table.
     *
     * @param table table to check
     * @return {@code true} if provided table is a vocabulary table, {@code false} otherwise
     */
    public static boolean isVocabularyTable(IOpenLTable table) {
        if (isDatatypeTable(table)) {
            var header = table.getSyntaxNode().getHeader().getSourceString();
            var len = header.length();
            int pos1 = StringUtils.first(header, 0, len, x -> x == VocabularyTableWriter.TYPE_OPEN);
            if (pos1 < 0) {
                return false;
            }
            int pos2 = StringUtils.first(header, pos1, len, x -> x == VocabularyTableWriter.TYPE_CLOSE);
            return pos1 < pos2;
        }
        return false;
    }

    /**
     * Checks if provided table is a datatype table.
     *
     * @param table table to check
     * @return {@code true} if provided table is a datatype table, {@code false} otherwise
     */
    public static boolean isDatatypeTable(IOpenLTable table) {
        return XlsNodeTypes.getEnumByValue(table.getType()) == XlsNodeTypes.XLS_DATATYPE;
    }

    /**
     * Checks if provided table is a simple rules table.
     *
     * @param table table to check
     * @return {@code true} if provided table is a simple rules table, {@code false} otherwise
     */
    public static boolean isSimpleRules(IOpenLTable table) {
        return isRules(table, SimpleRulesView.TABLE_TYPE);
    }

    /**
     * Checks if provided table is a smart rules table.
     *
     * @param table table to check
     * @return {@code true} if provided table is a smart rules table, {@code false} otherwise
     */
    public static boolean isSmartRules(IOpenLTable table) {
        return isRules(table, SmartRulesView.TABLE_TYPE);
    }

    private static boolean isRules(IOpenLTable table, String tableType) {
        if (isRulesTable(table)) {
            var headerSource = table.getSyntaxNode().getHeader().getSourceString();
            var first = StringUtils.firstNonSpace(headerSource, 0, headerSource.length());
            var last = StringUtils.first(headerSource, first, headerSource.length(), StringUtils::isSpaceOrControl);
            return first < last && Objects.equals(headerSource.substring(first, last), tableType);
        }
        return false;
    }

    /**
     * Checks if provided table is a rules table.
     *
     * @param table table to check
     * @return {@code true} if provided table is a rules table, {@code false} otherwise
     */
    public static boolean isRulesTable(IOpenLTable table) {
        return XlsNodeTypes.getEnumByValue(table.getType()) == XlsNodeTypes.XLS_DT;
    }

    /**
     * Checks if provided table is a simple spreadsheet table.
     *
     * @param table table to check
     * @return {@code true} if provided table is a simple spreadsheet table, {@code false} otherwise
     */
    public static boolean isSimpleSpreadsheet(IOpenLTable table) {
        if (isSpreadsheetTable(table)) {
            var tableBody = table.getSyntaxNode().getTableBody();
            int height = getHeightWithoutEmptyRows(tableBody);
            int width = getWidthWithoutEmptyColumns(tableBody);
            return width == 2 || height == 2;
        }
        return false;
    }

    /**
     * Checks if provided table is a spreadsheet table.
     *
     * @param table table to check
     * @return {@code true} if provided table is a spreadsheet table, {@code false} otherwise
     */
    public static boolean isSpreadsheetTable(IOpenLTable table) {
        return XlsNodeTypes.getEnumByValue(table.getType()) == XlsNodeTypes.XLS_SPREADSHEET;
    }

    /**
     * Gets height of provided table without empty rows.
     *
     * @param table table to get height
     * @return height of provided table without empty rows
     */
    public static int getHeightWithoutEmptyRows(ITable<?> table) {
        var height = table.getHeight();
        while (isRowEmpty(table, height - 1)) {
            height--;
        }
        return height;
    }

    private static boolean isRowEmpty(ITable<?> table, int row) {
        for (int col = 0; col < table.getWidth(); col++) {
            if (table.getCell(col, row).getObjectValue() != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets width of provided table without empty columns.
     *
     * @param table table to get width
     * @return width of provided table without empty columns
     */
    public static int getWidthWithoutEmptyColumns(ITable<?> table) {
        var width = table.getWidth();
        while (isColumnEmpty(table, width - 1)) {
            width--;
        }
        return width;
    }

    private static boolean isColumnEmpty(ITable<?> table, int col) {
        for (int row = 0; row < table.getHeight(); row++) {
            if (table.getCell(col, row).getObjectValue() != null) {
                return false;
            }
        }
        return true;
    }

}
