package org.openl.rules.commons.artefacts;

import org.openl.rules.commons.CommonException;

public class ArtefactException extends CommonException {
    public ArtefactException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ArtefactException(String msg, Object... params) {
        super(msg, params);
    }
}
