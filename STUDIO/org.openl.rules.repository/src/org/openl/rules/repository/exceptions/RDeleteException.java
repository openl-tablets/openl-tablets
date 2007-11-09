package org.openl.rules.repository.exceptions;

/**
 * OpenL Rules Delete Exception.
 *
 * @author Aleh Bykhavets
 *
 */
public class RDeleteException extends RRepositoryException {
    private static final long serialVersionUID = -3158589032887918458L;

    public RDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

    public RDeleteException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
