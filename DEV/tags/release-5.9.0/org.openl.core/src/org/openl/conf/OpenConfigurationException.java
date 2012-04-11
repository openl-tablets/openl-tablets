/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

// import org.apache.commons.lang.exception.NestableException;
import org.apache.commons.lang.exception.NestableRuntimeException;

/**
 * @author snshor
 *
 */

public class OpenConfigurationException extends NestableRuntimeException {
    // TODO add parameters, message etc.

    /**
     *
     */
    private static final long serialVersionUID = 3292629986027365336L;
    String uri;
    Throwable cause;

    public OpenConfigurationException(String msg, String uri, Throwable t) {
        super(msg, t);
    }

    /**
     * @return
     */
    public String getUri() {
        return uri;
    }

    @Override
    public String getMessage() {
        String myMsg = super.getMessage();
        
        if (myMsg == null)
            myMsg = "";
        
        
        if (uri != null)
            myMsg += " URI: " + uri;
        
        if (getCause() != null)
            myMsg += " Cause: " + getCause().getMessage();
        
        return myMsg;
    }
    
    

}
