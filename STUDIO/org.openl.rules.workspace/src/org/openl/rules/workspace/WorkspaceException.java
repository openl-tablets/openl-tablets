package org.openl.rules.workspace;

import org.openl.CommonException;

public class WorkspaceException extends CommonException {
    public WorkspaceException( String msg, Throwable cause) {
        super(msg, cause);
    }

    public WorkspaceException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
