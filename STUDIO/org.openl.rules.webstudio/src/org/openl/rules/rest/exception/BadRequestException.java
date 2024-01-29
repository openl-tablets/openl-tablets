package org.openl.rules.rest.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
@Hidden
public class BadRequestException extends RestRuntimeException {

    public BadRequestException(String code) {
        super(code);
    }

    public BadRequestException(String code, Object[] args) {
        super(code, args);
    }
}
