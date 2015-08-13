package org.openl.extension.xmlrules.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class RulesTableReference {
    private final CellReference reference;

    public RulesTableReference(CellReference reference) {
        this.reference = reference;
    }

    public String getTable() {
        return prepareString(reference.getWorkbook()) + "$" + prepareString(reference.getSheet());
    }

    public String getRow() {
        return reference.getRow();
    }

    public String getColumn() {
        return reference.getColumn();
    }

    private String prepareString(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        char[] chars = input.toCharArray();
        if (!isLetter(chars[0])) {
            sb.append('$');
        }
        for (char c : chars) {
            if (isLetter(c) || isDigit(c)) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static boolean isLetter(char c) {
        char symbols[][] = {
                {'\u0024'},
                {'\u0041', '\u005a'},
                {'\u005f'},
                {'\u0061', '\u007a'},
                {'\u00c0', '\u00d6'},
                {'\u00d8', '\u00f6'},
                {'\u00f8', '\u00ff'},
                {'\u0100', '\u1fff'},
                {'\u3040', '\u318f'},
                {'\u3300', '\u337f'},
                {'\u3400', '\u3d2d'},
                {'\u4e00', '\u9fff'},
                {'\uf900', '\ufaff'}
        };

        return isInRange(c, symbols);
    }

    private static boolean isDigit(char c) {
        char symbols[][] = {
                {'\u0030','\u0039'},
                {'\u0660','\u0669'},
                {'\u06f0','\u06f9'},
                {'\u0966','\u096f'},
                {'\u09e6','\u09ef'},
                {'\u0a66','\u0a6f'},
                {'\u0ae6','\u0aef'},
                {'\u0b66','\u0b6f'},
                {'\u0be7','\u0bef'},
                {'\u0c66','\u0c6f'},
                {'\u0ce6','\u0cef'},
                {'\u0d66','\u0d6f'},
                {'\u0e50','\u0e59'},
                {'\u0ed0','\u0ed9'},
                {'\u1040','\u1049'},
        };

        return isInRange(c, symbols);
    }

    private static boolean isInRange(char c, char[][] symbols) {
        for (char[] row : symbols) {
            if (row.length == 1) {
                if (row[0] == c) {
                    return true;
                }
            } else {
                if (row[0] <= c && c <= row[1]) {
                    return true;
                }
            }
        }

        return false;
    }
}
