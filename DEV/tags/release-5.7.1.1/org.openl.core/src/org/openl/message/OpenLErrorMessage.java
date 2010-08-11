package org.openl.message;

import org.apache.commons.lang.StringUtils;
import org.openl.exception.OpenLException;

/**
 * Class defines error OpenL message abstraction. <code>OpenLErrorMessage</code> encapsulates {@link IOpenLError} object
 * as source of message.
 * 
 */
public class OpenLErrorMessage extends OpenLMessage {

    private OpenLException error;

    public OpenLErrorMessage(String summary, String details) {
        super(summary, details, Severity.ERROR);
    }

    public OpenLErrorMessage(String summary, String details, OpenLException error) {
        super(summary, details, Severity.ERROR);

        this.error = error;
    }

    public OpenLErrorMessage(OpenLException error) {
        this(error.getOriginalMessage(), StringUtils.EMPTY, error);
    }

    public OpenLException getError() {
        return error;
    }

}
