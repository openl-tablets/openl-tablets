package org.openl.rules.maven.gen;

public class EnumInitializationWriter implements TypeInitializationWriter {

    public String getInitialization(Object value) {
        if (value.getClass().isEnum()) {
            Enum<?> e = (Enum<?>) value;
            return value.getClass().getSimpleName() + "." + e.name();
        }
        throw new IllegalStateException("Expected enum type!");
    }

}
