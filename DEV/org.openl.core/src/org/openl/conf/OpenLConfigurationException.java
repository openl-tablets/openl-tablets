/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

/**
 * @author snshor
 */

public class OpenLConfigurationException extends RuntimeException {
    // TODO add parameters, message etc.

    private static final long serialVersionUID = 3292629986027365336L;

    public OpenLConfigurationException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getMessage() {
        String myMsg = super.getMessage();

        if (myMsg == null) {
            myMsg = "";
        }

        if (getCause() != null) {
            myMsg += " Cause: " + getCause().getMessage();
        }

        return myMsg;
    }

}
