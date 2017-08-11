package org.openl.rules.maven.gen;

public class DefaultInitializationWriter implements TypeInitializationWriter {
    @Override
    public String getInitialization(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Not null value is expected!");
        }

        return String.format("new %s()", value.getClass().getCanonicalName());
    }
}
