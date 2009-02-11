package org.openl.rules.tbasic.runtime;

import org.openl.binding.impl.ControlSignal;

public class OpenLAlgorithmGoToMainSignal extends ControlSignal {

    private String label;
    
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

    /**
     * 
     */
    private static final long serialVersionUID = -4368227688146541082L;

}
