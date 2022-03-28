package org.openl.rules.rest.exception;

import java.util.Optional;

import org.springframework.core.annotation.AnnotationUtils;
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

    public HttpStatus getHttpStatus() {
        return Optional.ofNullable(AnnotationUtils.findAnnotation(getClass(), ResponseStatus.class))
            .map(ResponseStatus::code)
            .orElse(null);
    }

    public String getErrorCode() {
        var httpStatus = getHttpStatus();
        return httpStatus != null ? DEF_ERROR_PREFIX + httpStatus.value() + "." + code : code;
    }

    public Object[] getArgs() {
        return args;
    }

}
