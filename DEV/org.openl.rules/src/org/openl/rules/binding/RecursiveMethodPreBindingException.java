package org.openl.rules.binding;

import org.openl.exception.OpenlNotCheckedException;

public class RecursiveMethodPreBindingException extends OpenlNotCheckedException {

    private static final long serialVersionUID = -514314877350252563L;

    @Override
    public String getMessage() {
        StringBuilder buf = new StringBuilder();

        buf.append(
            "Automatically type definition for Spreadsheet with the circular reference is failed. Please, define manually common 'SpreadsheetResult' type for the cell.");

        return buf.toString();
    }
}