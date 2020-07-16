package org.openl.rules.project.abstraction;

import org.openl.rules.common.ProjectException;

public class LockException extends ProjectException {

    public LockException(String message, Throwable exception) {
        super(message, exception);
    }

    public LockException(String message) {
        super(message);
    }
}
