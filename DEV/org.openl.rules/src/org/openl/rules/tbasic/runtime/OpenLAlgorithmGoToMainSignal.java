package org.openl.rules.tbasic.runtime;

import org.openl.binding.impl.ControlSignal;

/**
 * Signal is used when executed GOTO operation from subroutine to main.
 *
 * @author User
 *
 */

@SuppressWarnings("serial")
public class OpenLAlgorithmGoToMainSignal extends ControlSignal {

    private String label;

    /**
     * Create an instance of <code>OpenLAlgorithmGoToMainSignal</code> initialized with label to jump.
     *
     * @param operations
     * @param labels
     */
    public OpenLAlgorithmGoToMainSignal(String label) {
        super();
        this.label = label;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }
}
