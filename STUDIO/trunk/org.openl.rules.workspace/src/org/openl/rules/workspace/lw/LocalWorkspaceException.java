package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.abstracts.ProjectException;

public class LocalWorkspaceException extends ProjectException {
    private static final long serialVersionUID = -2329484055042509751L;

    public LocalWorkspaceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public LocalWorkspaceException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
