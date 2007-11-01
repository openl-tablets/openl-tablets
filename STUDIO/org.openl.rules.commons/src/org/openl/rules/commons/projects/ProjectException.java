package org.openl.rules.commons.projects;

import org.openl.rules.commons.CommonException;

public class ProjectException extends CommonException {
    public ProjectException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ProjectException(String msg, Object... params) {
        super(msg, params);
    }
}
