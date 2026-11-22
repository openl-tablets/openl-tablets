/**
 *
 */
package org.openl.rules.tbasic.runtime;

import java.io.Serial;

/**
 * @author User
 */
public class OpenLAlgorithmExecutionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -6258532512965806620L;

    public OpenLAlgorithmExecutionException() {
    }

    /**
     * @param message
     */
    public OpenLAlgorithmExecutionException(String message) {
        super(message);
    }

}
