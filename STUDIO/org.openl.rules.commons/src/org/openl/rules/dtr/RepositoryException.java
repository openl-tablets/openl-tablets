package org.openl.rules.dtr;

import org.openl.rules.commons.projects.ProjectException;

public class RepositoryException extends ProjectException {
    public RepositoryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RepositoryException(String msg, Object... params) {
        super(msg, params);
    }
}
