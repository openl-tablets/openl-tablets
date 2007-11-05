package org.openl.rules.workspace.abstracts;

import org.openl.CommonException;

public class ProjectException extends CommonException {
    public ProjectException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ProjectException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
