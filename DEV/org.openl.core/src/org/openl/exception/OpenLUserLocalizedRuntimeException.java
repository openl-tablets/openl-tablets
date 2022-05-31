package org.openl.exception;

public class OpenLUserLocalizedRuntimeException extends OpenLUserRuntimeException {

    private final String code;
    private final Object[] args;

    public OpenLUserLocalizedRuntimeException(String code, String message, Object... args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }

}
