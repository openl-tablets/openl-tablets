package org.openl.rules.binding;

import java.util.Objects;

import org.openl.exception.OpenlNotCheckedException;

public class RecursiveSpreadsheetMethodPreBindingException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 1L;

    private final String spreadsheetMethodName;

    public RecursiveSpreadsheetMethodPreBindingException(String spreadsheetMethodName) {
        super();
        this.spreadsheetMethodName = Objects.requireNonNull(spreadsheetMethodName,
            "spreadsheetMethodName cannot be null");
    }

    @Override
    public String getMessage() {
        return String.format(
            "Automatically type definition for Spreadsheet '%s' with the circular reference is failed. Please, define common 'SpreadsheetResult' type.",
            spreadsheetMethodName);
    }

}
