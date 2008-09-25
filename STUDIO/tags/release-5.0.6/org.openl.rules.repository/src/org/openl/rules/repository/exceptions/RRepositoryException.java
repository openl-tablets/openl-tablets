package org.openl.rules.repository.exceptions;

import java.text.MessageFormat;

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
    
    public RRepositoryException(String pattern, Throwable cause, Object... params) {
        super(format(pattern, params), cause);
    }
    
    // --- private
    
    private static String format(String pattern, Object... params) {
        return MessageFormat.format(pattern, params);
    }
}
