package org.openl.rules.workspace.deploy;

import org.openl.rules.workspace.abstracts.ProjectException;


public class DeploymentException extends ProjectException{
    private static final long serialVersionUID = -3106574082492867820L;

    public DeploymentException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DeploymentException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
