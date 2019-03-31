package org.openl.rules.common;

import java.text.MessageFormat;

/**
 * There are no CommonException(String pattern, Object... params) constructor since it will lead to ambiguous case with
 * Throwable. Thus, Throwable can be placed in "Object..." and will be treated like parameter for result message, not a
 * <code>cause</code>. That kind of bug is hard to detect.
 *
 * If you cannot provide Throwable just use <code>null</code>.
 *
 * @author Aleh Bykhavets
 *
 */
public class CommonException extends Exception {
    private static final long serialVersionUID = 9016192638697492055L;

    private static String format(String pattern, Object... params) {
        return MessageFormat.format(pattern, params);
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *            method.
     */
    public CommonException(String message) {
        super(message);
    }

    public CommonException(String msg, Throwable cause) {
        super(msg, cause);
    }

    // --- private

    public CommonException(String pattern, Throwable cause, Object... params) {
        super(format(pattern, params), cause);
    }
}
