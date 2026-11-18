package org.openl.rules.binding;

import java.io.Serial;

import org.openl.exception.OpenlNotCheckedException;

public class RecursiveSpreadsheetMethodPreBindingException extends OpenlNotCheckedException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RecursiveSpreadsheetMethodPreBindingException(String message) {
        super(message);
    }

}
