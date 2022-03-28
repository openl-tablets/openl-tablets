package org.openl.rules.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Hidden;

// TODO: get rid of this class. All validation must be done via custom validators!!!
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
@Hidden
@Deprecated
public class BadRequestException extends RestRuntimeException {

    public BadRequestException(String code) {
        super(code);
    }

}
