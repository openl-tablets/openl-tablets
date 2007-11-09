package org.openl.rules.repository.exceptions;

/**
 * OpenL Rules Modify Exception
 *
 * @author Aleh Bykhavets
 *
 */
public class RModifyException extends RRepositoryException {
    private static final long serialVersionUID = -5137354292230021604L;

    public RModifyException(String message, Throwable cause) {
        super(message, cause);
    }

    public RModifyException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
