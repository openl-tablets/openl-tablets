package org.openl.exception;

import java.io.Serial;

/**
 * Parent for OpenL Java exceptions.
 *
 * <p>Such an exception points at no place of a source: its location, source code and source location are none.
 */
public class OpenLCheckedException extends Exception implements OpenLException {

    @Serial
    private static final long serialVersionUID = -4044064134031015107L;

    public OpenLCheckedException(String message) {
        super(message);
    }

    public OpenLCheckedException(Throwable cause) {
        // the message stays none, not the text of the cause
        super(null, cause);
    }

    public OpenLCheckedException(String message, Throwable cause) {
        super(message, cause);
    }
}
