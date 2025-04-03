package org.openl.rules.webstudio.web.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.openl.rules.common.CommonException;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class RepositoryValidationException extends CommonException {

    private static final long serialVersionUID = -7067195368061113686L;

    public RepositoryValidationException(String message) {
        super(message);
    }

    public RepositoryValidationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RepositoryValidationException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }

}
