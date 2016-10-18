package org.openl.rules.binding;

import org.openl.exception.OpenlNotCheckedException;

public class RecursiveMethodPreBindingException extends OpenlNotCheckedException {

    private static final long serialVersionUID = -514314877350252563L;

    public RecursiveMethodPreBindingException() {
    }

    @Override
    public String getMessage() {
        StringBuilder buf = new StringBuilder();

        buf.append("Сustom Spreadsheet Type can't be defined correctly with the circular reference. Please, define manually common SpreadsheetResult type for the cell.");

        return buf.toString();
    }
}