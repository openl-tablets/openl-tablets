package org.openl.rules.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.LOCKED)
public class LockedException extends RestRuntimeException {

    public LockedException(String code) {
        super(code);
    }

}
