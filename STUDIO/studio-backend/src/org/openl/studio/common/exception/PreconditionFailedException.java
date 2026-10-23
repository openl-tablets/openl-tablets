package org.openl.studio.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A request is refused because the condition it names does not hold, such as an {@code If-Match} header naming
 * another version than the current one. Nothing is changed.
 */
@ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
public class PreconditionFailedException extends RestRuntimeException {

    public PreconditionFailedException(String code, Object... args) {
        super(code, args);
    }

}
