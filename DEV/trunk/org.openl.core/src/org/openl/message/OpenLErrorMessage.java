package org.openl.message;

import org.apache.commons.lang.StringUtils;
import org.openl.exception.OpenLCompilationException;

/**
 * Class defines error OpenL message abstraction. <code>OpenLErrorMessage</code> encapsulates {@link IOpenLError} object
 * as source of message.
 * 
 */
public class OpenLErrorMessage extends OpenLMessage {

    private OpenLCompilationException error;

    public OpenLErrorMessage(String summary, String details) {
        super(summary, details, null);
    }

    public OpenLErrorMessage(String summary, String details, OpenLCompilationException error) {
        super(summary, details, Severity.ERROR);

        this.error = error;
    }

    public OpenLErrorMessage(OpenLCompilationException error) {
        this(error.getMessage(), StringUtils.EMPTY, error);
    }

    public OpenLCompilationException getError() {
        return error;
    }

}
