package org.openl.rules.workspace.dtr;

import org.openl.rules.workspace.abstracts.ProjectException;

public class RepositoryException extends ProjectException {
    public RepositoryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RepositoryException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
