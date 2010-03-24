package org.openl.message;

import org.apache.commons.lang.StringUtils;
import org.openl.error.IOpenLError;

/**
 * Class defines error OpenL message abstraction. <code>OpenLErrorMessage</code> encapsulates {@link IOpenLError} object
 * as source of message.
 * 
 */
public class OpenLErrorMessage extends OpenLMessage {

    private IOpenLError error;

    public OpenLErrorMessage(String summary, String details) {
        super(summary, details, null);
    }

    public OpenLErrorMessage(String summary, String details, IOpenLError error) {
        super(summary, details, Severity.ERROR);

        this.error = error;
    }

    public OpenLErrorMessage(IOpenLError error) {
        this(error.getMessage(), StringUtils.EMPTY, error);
    }

    public IOpenLError getError() {
        return error;
    }

}
