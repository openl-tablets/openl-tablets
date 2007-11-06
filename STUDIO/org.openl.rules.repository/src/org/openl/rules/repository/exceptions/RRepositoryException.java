package org.openl.rules.repository.exceptions;

/**
 * OpenL Rules Repository Exception
 *
 * @author Aleh Bykhavets
 *
 */
public class RRepositoryException extends Exception {
    private static final long serialVersionUID = 8258995781116021601L;

    public RRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
