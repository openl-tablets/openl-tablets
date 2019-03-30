package org.openl.runtime;

public class DefaultRuntimeContext implements IRuntimeContext {

    @Override
    public IRuntimeContext clone() throws CloneNotSupportedException {
        return this;
    }

}
