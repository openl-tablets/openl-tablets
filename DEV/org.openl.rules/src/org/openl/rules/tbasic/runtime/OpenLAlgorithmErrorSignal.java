/**
 *
 */
package org.openl.rules.tbasic.runtime;

import org.openl.binding.impl.ControlSignal;

/**
 * @author User
 */
@SuppressWarnings("serial")
public class OpenLAlgorithmErrorSignal extends ControlSignal {

    /**
     * @param cause
     */
    public OpenLAlgorithmErrorSignal(Throwable cause) {
        super(cause);
    }

}
