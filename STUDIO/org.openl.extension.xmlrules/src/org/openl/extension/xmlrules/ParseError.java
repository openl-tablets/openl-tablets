package org.openl.extension.xmlrules;

public class ParseError {
    private final int row;
    private final int column;
    private final String message;

    public ParseError(int row, int column, String message) {
        this.row = row;
        this.column = column;
        this.message = message;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public String getMessage() {
        return message;
    }
}
