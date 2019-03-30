/**
 *
 */
package org.openl.rules.tbasic.runtime;

/**
 * @author User
 *
 */
public class OpenLAlgorithmExecutionException extends RuntimeException {

    private static final long serialVersionUID = -6258532512965806620L;

    public OpenLAlgorithmExecutionException() {
    }

    /**
     * @param message
     */
    public OpenLAlgorithmExecutionException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public OpenLAlgorithmExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public OpenLAlgorithmExecutionException(Throwable cause) {
        super(cause);
    }

}
