/**
 * 
 */
package org.openl.rules.tbasic.runtime;

import org.openl.binding.impl.ControlSignal;

/**
 * @author User
 * 
 */
public class OpenLAlgorithmErrorSignal extends ControlSignal {
    /**
     * 
     */
    private static final long serialVersionUID = -7447652554311281470L;

    /**
     * 
     */
    public OpenLAlgorithmErrorSignal() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     */
    public OpenLAlgorithmErrorSignal(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     */
    public OpenLAlgorithmErrorSignal(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     */
    public OpenLAlgorithmErrorSignal(Throwable cause) {
        super(cause);
    }

}
