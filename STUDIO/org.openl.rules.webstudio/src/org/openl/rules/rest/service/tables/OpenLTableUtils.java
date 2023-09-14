package org.openl.rules.rest.service.tables;

import java.util.Objects;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.rest.model.tables.SimpleRulesView;
import org.openl.rules.rest.service.tables.write.VocabularyTableWriter;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.util.StringUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

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

    public static boolean isDatatypeTable(IOpenLTable table) {
        return XlsNodeTypes.getEnumByValue(table.getType()) == XlsNodeTypes.XLS_DATATYPE;
    }

    public static boolean isSimpleRules(IOpenLTable table) {
        if (isRulesTable(table)) {
            var headerSource = table.getSyntaxNode().getHeader().getSourceString();
            var first = StringUtils.firstNonSpace(headerSource, 0, headerSource.length());
            var last = StringUtils.first(headerSource, first, headerSource.length(), StringUtils::isSpaceOrControl);
            return first < last && Objects.equals(headerSource.substring(first, last), SimpleRulesView.TABLE_TYPE);
        }
        return false;
    }

    public static boolean isRulesTable(IOpenLTable table) {
        return XlsNodeTypes.getEnumByValue(table.getType()) == XlsNodeTypes.XLS_DT;
    }

    public static boolean isSimpleSpreadsheet(IOpenLTable table) {
        if (isSpreadsheetTable(table)) {
            var tableBody = table.getSyntaxNode().getTableBody();
            int height = getHeightWithoutEmptyRows(tableBody);
            int width = getWidthWithoutEmptyColumns(tableBody);
            return width == 2 || height == 2;
        }
        return false;
    }

    public static boolean isSpreadsheetTable(IOpenLTable table) {
        return XlsNodeTypes.getEnumByValue(table.getType()) == XlsNodeTypes.XLS_SPREADSHEET;
    }

    public static int getHeightWithoutEmptyRows(ILogicalTable table) {
        var height = table.getHeight();
        while (isRowEmpty(table, height - 1)) {
            height--;
        }
        return height;
    }

    private static boolean isRowEmpty(ILogicalTable table, int row) {
        for (int col = 0; col < table.getWidth(); col++) {
            if (table.getCell(col, row).getObjectValue() != null) {
                return false;
            }
        }
        return true;
    }

    public static int getWidthWithoutEmptyColumns(ILogicalTable table) {
        var width = table.getWidth();
        while (isColumnEmpty(table, width - 1)) {
            width--;
        }
        return width;
    }

    private static boolean isColumnEmpty(ILogicalTable table, int col) {
        for (int row = 0; row < table.getHeight(); row++) {
            if (table.getCell(col, row).getObjectValue() != null) {
                return false;
            }
        }
        return true;
    }

}
