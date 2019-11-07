package org.openl.rules.binding;

import org.openl.exception.OpenlNotCheckedException;

public class RecursiveSpreadsheetMethodPreBindingException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 1L;

    public RecursiveSpreadsheetMethodPreBindingException() {
        super();
    }

    @Override
    public String getMessage() {
        return "Automatically type definition for Spreadsheet with the circular reference is failed. Please, define manually common 'SpreadsheetResult' type for the cell.";
    }

}
