package org.openl.rules.rest;

import org.openl.rules.rest.exception.ForbiddenException;

class SecurityException extends ForbiddenException {

    SecurityException() {
        super("default.message");
    }

}
