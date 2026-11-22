package org.openl.rules.webstudio.web.repository.upload;

import org.openl.rules.common.ProjectException;

public class OpenAPIProjectException extends ProjectException {

    public OpenAPIProjectException(String message) {
        super(message);
    }

    public OpenAPIProjectException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
