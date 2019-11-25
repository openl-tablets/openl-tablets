/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

/**
 * @author snshor
 *
 */

public class OpenLConfigurationException extends RuntimeException {
    // TODO add parameters, message etc.

    private static final long serialVersionUID = 3292629986027365336L;
    private final String uri;

    public OpenLConfigurationException(String msg, String uri, Throwable t) {
        super(msg, t);
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String getMessage() {
        String myMsg = super.getMessage();

        if (myMsg == null) {
            myMsg = "";
        }

        if (uri != null) {
            myMsg += " URI: " + uri;
        }

        if (getCause() != null) {
            myMsg += " Cause: " + getCause().getMessage();
        }

        return myMsg;
    }

}
