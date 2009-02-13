package org.openl.rules.tbasic.runtime;

import org.openl.binding.impl.ControlSignal;

@SuppressWarnings("serial")
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



}
