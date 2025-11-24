package org.openl.rules.rest;

import org.openl.rules.rest.exception.ForbiddenException;

public class SecurityException extends ForbiddenException {

    public SecurityException() {
        super("default.message");
    }

}
