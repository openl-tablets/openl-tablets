package org.openl.rules.repository.exceptions;

import org.openl.rules.common.ProjectException;

/**
 * OpenL Rules Repository Exception
 *
 * @author Aleh Bykhavets
 *
 */
public class RRepositoryException extends ProjectException {
    private static final long serialVersionUID = 8258995781116021601L;

    public RRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

}
