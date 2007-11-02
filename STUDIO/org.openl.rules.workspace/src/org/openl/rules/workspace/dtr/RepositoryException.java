package org.openl.rules.workspace.dtr;

import org.openl.rules.workspace.abstracts.ProjectException;

public class RepositoryException extends ProjectException {
    public RepositoryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RepositoryException(String msg, Object... params) {
        super(msg, params);
    }
}
