package org.openl.rules.maven.gen;

public class StringInitializationWriter extends CommonInitializationWriter {

    @Override
    public String getInitialization(Object value) {
        return String.format("\"%s\"", super.getInitialization(value));
    }
}
