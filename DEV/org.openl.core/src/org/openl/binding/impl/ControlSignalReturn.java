/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

/**
 * @author snshor
 *
 */
public class ControlSignalReturn extends ControlSignal {

    private final Object returnValue;

    public ControlSignalReturn(Object returnValue) {
        this.returnValue = returnValue;
    }

    public Object getReturnValue() {
        return returnValue;
    }

}
