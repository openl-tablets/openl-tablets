package org.openl.exception;

import org.jspecify.annotations.Nullable;

import org.openl.source.SourceRange;
import org.openl.util.text.ILocation;

public interface OpenLException {

    String getMessage();

    Throwable getCause();

    /**
     * Gets error cause location.
     *
     * @return error cause location, or {@code null} when the error points at no source
     */
    default @Nullable ILocation getLocation() {
        return null;
    }

    /**
     * Gets source code module where the error was occurred.
     *
     * @return source code module, or {@code null} when the error points at no source
     */
    default @Nullable String getSourceCode() {
        return null;
    }

    /**
     * Gets error source location URI
     *
     * @return source location URI, or {@code null} when the error points at no source
     */
    default @Nullable String getSourceLocation() {
        return null;
    }

    /**
     * Gets the piece of the source the error is about, such as a part of the text of a cell.
     *
     * @return the piece, or {@code null} when the error points at no source
     */
    default @Nullable SourceRange getSourceRange() {
        return null;
    }

}
