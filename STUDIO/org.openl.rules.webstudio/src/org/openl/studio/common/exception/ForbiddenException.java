package org.openl.studio.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
public class ForbiddenException extends RestRuntimeException {

    public ForbiddenException() {
        this("default.message");
    }

    public ForbiddenException(String code) {
        super(code);
    }

    public ForbiddenException(String code, Object[] args) {
        super(code, args);
    }

}
