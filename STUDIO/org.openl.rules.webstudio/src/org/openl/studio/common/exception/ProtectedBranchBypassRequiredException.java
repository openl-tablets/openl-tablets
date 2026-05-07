package org.openl.studio.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals that the requested operation targets a protected branch and the current user is
 * eligible to bypass the protection (Manager role + global setting enabled), but did not
 * include the explicit {@code force=true} confirmation. Clients are expected to surface a
 * destructive-action confirmation and retry with {@code force=true}.
 */
@ResponseStatus(code = HttpStatus.CONFLICT)
public class ProtectedBranchBypassRequiredException extends RestRuntimeException {

    public ProtectedBranchBypassRequiredException(String code) {
        super(code);
    }

    public ProtectedBranchBypassRequiredException(String code, Object... args) {
        super(code, args);
    }
}
