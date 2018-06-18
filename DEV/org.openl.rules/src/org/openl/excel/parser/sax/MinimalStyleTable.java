package org.openl.excel.parser.sax;

import java.util.*;

import org.apache.poi.ss.usermodel.BuiltinFormats;

public class MinimalStyleTable {
    /**
     * Contains number format indexes only.
     */
    private List<Integer> numberFormatIdList = new ArrayList<>();
    private Map<Integer, Short> indentMap = new HashMap<>();

    /**
     * Contains number format string for each number format index.
     */
    private SortedMap<Integer, String> formatStrings = new TreeMap<>();

    public NumberFormat getFormat(int styleIndex) {
        if (numberFormatIdList.isEmpty()) {
            return null;
        }

        if (styleIndex < 0 || styleIndex > numberFormatIdList.size()) {
            styleIndex = 0;
        }

        Integer numberFormatId = numberFormatIdList.get(styleIndex);
        String formatString = formatStrings.get(numberFormatId);
        if (formatString == null) {
            formatString = BuiltinFormats.getBuiltinFormat(numberFormatId);
        }

        return new NumberFormat(numberFormatId, formatString);
    }

    public Short getIndent(int styleIndex) {
        return indentMap.get(styleIndex);
    }

    void addStyle(Integer numberFormatId) {
        numberFormatIdList.add(numberFormatId);
    }

    void addFormatString(Integer numberFormatId, String formatString) {
        formatStrings.put(numberFormatId, formatString);
    }

    void addIndent(short indent) {
        int lastStyleIndex = numberFormatIdList.size() - 1;
        indentMap.put(lastStyleIndex, indent);
    }
}
