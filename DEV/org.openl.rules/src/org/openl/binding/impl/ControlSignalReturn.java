/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import lombok.RequiredArgsConstructor;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class ControlSignalReturn extends ControlSignal {

    private final Object returnValue;

    public Object getReturnValue() {
        return returnValue;
    }

}
