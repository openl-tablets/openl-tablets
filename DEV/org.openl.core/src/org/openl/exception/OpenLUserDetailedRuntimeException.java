package org.openl.exception;

public class OpenLUserDetailedRuntimeException extends OpenLUserRuntimeException {

    private final String code;

    public OpenLUserDetailedRuntimeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getFullMessage() {
        return String.format("%s: %s", code, super.getMessage());
    }

}
