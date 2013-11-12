package org.openl.rules.repository.exceptions;

import java.text.MessageFormat;

import org.openl.rules.common.ProjectException;

/**
 * OpenL Rules Repository Exception
 *
 * @author Aleh Bykhavets
 *
 */
public class RRepositoryException extends ProjectException {
    private static final long serialVersionUID = 8258995781116021601L;

    private static String format(String pattern, Object... params) {
        return MessageFormat.format(pattern, params);
    }

    public RRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    // --- private

    public RRepositoryException(String pattern, Throwable cause, Object... params) {
        super(format(pattern, params), cause);
    }
}
