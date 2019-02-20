package org.openl.rules.project.abstraction;

import org.openl.rules.common.ProjectException;

public class LockException extends ProjectException {

    public LockException(String mesage, Throwable exception) {
        super(mesage, exception);
    }
}
