package org.openl.rules.lang.xls.syntax;

import java.util.Objects;

import org.openl.exception.OpenLCompilationException;

final class ErrorKey {
    private final String message;
    private final String sourceLocation;

    ErrorKey(OpenLCompilationException e) {
        message = e.getMessage();
        sourceLocation = e.getSourceLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ErrorKey errorKey = (ErrorKey) o;
        return Objects.equals(message, errorKey.message) && Objects.equals(sourceLocation, errorKey.sourceLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, sourceLocation);
    }
}
