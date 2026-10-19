package org.openl.studio.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * What a request names is no more: it was there once and was deleted.
 *
 * @author Yury Molchan
 */
@ResponseStatus(code = HttpStatus.GONE)
public class GoneException extends RestRuntimeException {

    public GoneException(String code) {
        super(code);
    }
}
