package org.openl.rules;

import org.openl.rules.commons.CommonException;

public class WorkspaceException extends CommonException {
    public WorkspaceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public WorkspaceException(String msg, Object... params) {
        super(msg, params);
    }
}
