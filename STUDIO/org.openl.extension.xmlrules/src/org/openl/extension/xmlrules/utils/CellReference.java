package org.openl.extension.xmlrules.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.extension.xmlrules.XmlRulesPath;
import org.openl.extension.xmlrules.model.single.node.RangeNode;

public class CellReference {
    private static final Pattern PATH_PATTERN = Pattern.compile("'?(\\[(.+)\\])?(([^']+)!?)?'?");
    private static final Pattern R1C1_PATTERN = Pattern.compile("'?(\\[(.+)\\])?(([^']+)'?!)?R(\\d+)C(\\d+)");
    private static final Pattern A1_PATTERN = Pattern.compile("'?(\\[(.+)\\])?(([^']+)'?!)?\\$?([A-Z]+)\\$?(\\d+)");
    private final String workbook;
    private final String sheet;
    private final String row;
    private final String column;

    private String escapedWorkbook;
    private String escapedSheet;

    public static CellReference parse(String reference) {
        return parse(null, null, reference);
    }

    public static CellReference parse(String currentWorkbook, String currentSheet, String reference) {
        if (reference == null) {
            String suffix = "";
            if (currentWorkbook != null) {
                suffix = " in workbook '" + currentWorkbook + "', sheet '" + currentSheet + "'";
            }
            throw new IllegalArgumentException("Empty cell reference" + suffix);
        }

        String workbook;
        String sheet;
        String row;
        String column;

        Matcher matcher = R1C1_PATTERN.matcher(reference);

        if (matcher.matches()) {
            workbook = matcher.group(2);

            sheet = matcher.group(4);
            row = matcher.group(5);
            column = matcher.group(6);
        } else {
            matcher = A1_PATTERN.matcher(reference);
            if (matcher.matches()) {
                workbook = matcher.group(2);

                sheet = matcher.group(4);
                row = matcher.group(6);
                column = String.valueOf(getColumnNumber(matcher.group(5)));
            } else {
                throw new IllegalArgumentException("Incorrect cell reference '" + reference + "'");
            }
        }

        if (workbook == null) {
            workbook = currentWorkbook;
        }
        if (sheet == null) {
            sheet = currentSheet;
        }

        return new CellReference(workbook, sheet, row, column);
    }

    public static CellReference parse(String currentWorkbook, String currentSheet, RangeNode rangeNode) {
        if (rangeNode == null) {
            String suffix = "";
            if (currentWorkbook != null) {
                suffix = " in workbook '" + currentWorkbook + "', sheet '" + currentSheet + "'";
            }
            throw new IllegalArgumentException("Empty cell reference" + suffix);
        }

        String workbook = null;
        String sheet = null;

        if (rangeNode.getPath() != null) {
            Matcher matcher = PATH_PATTERN.matcher(rangeNode.getPath());

            if (matcher.matches()) {
                workbook = matcher.group(2);
                sheet = matcher.group(4);
            }
        }
        if (workbook == null) {
            workbook = currentWorkbook;
        }
        if (sheet == null) {
            sheet = currentSheet;
        }
        return new CellReference(workbook, sheet, rangeNode.getRow(), rangeNode.getColumn());
    }

    public static CellReference parse(XmlRulesPath path, int row, int column) {
        return new CellReference(path.getWorkbook(), path.getSheet(), "" + row, "" + column);
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

    public int getRowNumber() {
        return Integer.parseInt(row);
    }

    public int getColumnNumber() {
        return Integer.parseInt(column);
    }

    public String getStringValue() {
        return "[" + workbook + "]" + sheet + "!R" + row + "C" + column;
    }

    private static int getColumnNumber(String letterColumn) {
        int result = 0;
        for (int i = 0; i < letterColumn.length(); i++) {
            result *= 26;
            result += letterColumn.charAt(i) - 'A' + 1;
        }
        return result;
    }

    public String getEscapedWorkbook() {
        if (escapedWorkbook == null) {
            escapedWorkbook = RulesTableReference.prepareString(workbook);
        }
        return escapedWorkbook;
    }

    public String getEscapedSheet() {
        if (escapedSheet == null) {
            escapedSheet = RulesTableReference.prepareString(sheet);
        }
        return escapedSheet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CellReference that = (CellReference) o;

        if (!workbook.equals(that.workbook))
            return false;
        if (!sheet.equals(that.sheet))
            return false;
        if (!row.equals(that.row))
            return false;
        return column.equals(that.column);

    }

    @Override
    public int hashCode() {
        int result = workbook.hashCode();
        result = 31 * result + sheet.hashCode();
        result = 31 * result + row.hashCode();
        result = 31 * result + column.hashCode();
        return result;
    }
}
