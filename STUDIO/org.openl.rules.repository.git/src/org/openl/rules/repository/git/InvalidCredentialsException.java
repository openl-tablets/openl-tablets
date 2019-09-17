package org.openl.rules.repository.git;

class InvalidCredentialsException extends IllegalArgumentException {
    InvalidCredentialsException(String s) {
        super(s);
    }
}
