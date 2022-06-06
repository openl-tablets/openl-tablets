package org.openl.exception;

public class OpenLUserLocalizedRuntimeException extends OpenLUserRuntimeException {

    private final String code;

    public OpenLUserLocalizedRuntimeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
