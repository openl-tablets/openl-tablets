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
public class ControlSignal extends RuntimeException {

    public ControlSignal() {
        super();
    }

    public ControlSignal(String message) {
        super(message);
    }

    public ControlSignal(String message, Throwable cause) {
        super(message, cause);
    }

    public ControlSignal(Throwable cause) {
        super(cause);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
