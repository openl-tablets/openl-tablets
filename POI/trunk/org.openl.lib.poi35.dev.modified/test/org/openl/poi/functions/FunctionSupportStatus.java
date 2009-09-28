package org.openl.poi.functions;

import java.util.ArrayList;
import java.util.List;

public enum FunctionSupportStatus {
    NON_IMPLEMENTED("Non-impemented."),
    NOT_TESTED("Not tested.(That means function can be not_impemented.)"),
    TESTED_WITH_ERRORS(""),
    SUPPORTED("Supported.");
    private String message;
    private List<String> errors = new ArrayList<String>();

    private FunctionSupportStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    };
}
