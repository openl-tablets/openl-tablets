package org.openl.rules.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RestRuntimeException extends RuntimeException {

    private static final String DEF_ERROR_PREFIX = "openl.error.";

    private final String code;
    private final Object[] args;

    public RestRuntimeException(String code) {
        this(code, null, null);
    }

    public RestRuntimeException(String code, Object[] args) {
        this(code, args, null);
    }

    public RestRuntimeException(String code, Object[] args, String message) {
        super(message);
        this.code = code;
        this.args = args;
    }

    public String getErrorCode() {
        ResponseStatus status = getClass().getAnnotation(ResponseStatus.class);
        if (status != null) {
            return DEF_ERROR_PREFIX + status.code().value() + "." + code;
        } else {
            return code;
        }
    }

    public Object[] getArgs() {
        return args;
    }

}
