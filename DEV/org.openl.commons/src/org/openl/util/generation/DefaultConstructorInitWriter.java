package org.openl.util.generation;

/**
 * Created by dl on 11/27/14.
 */
public class DefaultConstructorInitWriter implements TypeInitializationWriter {
    @Override
    public String getInitialization(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        return String.format("new %s()", value.getClass().getCanonicalName());
    }
}
