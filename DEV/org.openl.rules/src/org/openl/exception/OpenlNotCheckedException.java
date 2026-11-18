package org.openl.exception;

import java.io.Serial;

import lombok.NoArgsConstructor;

/**
 * Parent for OpenL Java runtime exceptions.
 *
 * <p>Such an exception points at no place of a source: its location, source code and source location are none.
 */
@NoArgsConstructor
public class OpenlNotCheckedException extends RuntimeException implements OpenLException {

    @Serial
    private static final long serialVersionUID = -4044064134031015107L;

    public OpenlNotCheckedException(String message) {
        super(message);
    }

    public OpenlNotCheckedException(Throwable cause) {
        // the message stays none, not the text of the cause
        super(null, cause);
    }

    public OpenlNotCheckedException(String message, Throwable cause) {
        super(message, cause);
    }
}
