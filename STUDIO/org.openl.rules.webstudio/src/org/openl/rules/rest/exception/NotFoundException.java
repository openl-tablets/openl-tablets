package org.openl.rules.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class NotFoundException extends RestRuntimeException {

    public NotFoundException(String code) {
        super(code);
    }

    public NotFoundException(String code, Object... args) {
        super(code, args);
    }
}
