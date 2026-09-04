package org.openl.studio.common.exception;

import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequiredArgsConstructor
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RestRuntimeException extends RuntimeException {

    private static final String DEF_ERROR_PREFIX = "openl.error.";

    private final String code;
    @Getter
    private final Object[] args;

    public RestRuntimeException(String code) {
        this(code, null);
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

}
