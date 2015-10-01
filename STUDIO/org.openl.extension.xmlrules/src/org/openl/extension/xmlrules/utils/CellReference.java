package org.openl.extension.xmlrules.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CellReference {
    private static final Pattern PATTERN = Pattern.compile("(\\[(.+)\\])?((.+)!)?\\$?([A-Z]+)\\$?(\\d+)");
    private final String workbook;
    private final String sheet;
    private final String row;
    private final String column;

    public static CellReference parse(String reference) {
        return parse(null, null, reference);
    }

    public static CellReference parse(String currentWorkbook, String currentSheet, String reference) {
        if (reference == null) {
            throw new IllegalArgumentException("Empty cell reference");
        }
        Matcher matcher = PATTERN.matcher(reference);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Incorrect cell reference '" + reference + "'");
        }

        String workbook = matcher.group(2);
        if (workbook == null) {
            workbook = currentWorkbook;
        }
        String sheet = matcher.group(4);
        if (sheet == null) {
            sheet = currentSheet;
        }
        return new CellReference(workbook, sheet, matcher.group(6), matcher.group(5));
    }

    public CellReference(String workbook, String sheet, String row, String column) {
        this.workbook = workbook;
        this.sheet = sheet;
        this.row = row;
        this.column = column;
    }

    public String getWorkbook() {
        return workbook;
    }

    public String getSheet() {
        return sheet;
    }

    public String getRow() {
        return row;
    }

    public String getColumn() {
        return column;
    }

    public String getStringValue() {
        return String.format("[%s]%s!%s%s", workbook, sheet, column, row);
    }
}
