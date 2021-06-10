package org.openl.rules.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class ConflictException extends RestRuntimeException {

    public ConflictException(String code) {
        super(code);
    }

    public ConflictException(String code, Object[] args) {
        super(code, args);
    }
}
