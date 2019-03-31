package org.openl.rules.workspace.deploy;

import org.openl.rules.common.ProjectException;

/**
 * An exception related to deployment to production repository and working with deployed projects there.
 */
public class DeploymentException extends ProjectException {
    private static final long serialVersionUID = -3106574082492867820L;

    public DeploymentException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DeploymentException(String pattern, Throwable cause, Object... params) {
        super(pattern, cause, params);
    }
}
