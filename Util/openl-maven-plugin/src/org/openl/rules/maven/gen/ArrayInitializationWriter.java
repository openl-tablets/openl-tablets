package org.openl.rules.maven.gen;

import java.lang.reflect.Array;

public class ArrayInitializationWriter implements TypeInitializationWriter {
    @Override
    public String getInitialization(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Not null value is expected!");
        }

        if (value.getClass().isArray() && !value.getClass().getComponentType().isArray()) {

            TypeInitializationWriter writer = SimpleBeanJavaGenerator
                .geTypeInitializationWriter(value.getClass().getComponentType());

            StringBuilder sb = new StringBuilder();

            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Object v = Array.get(value, i);
                sb.append(writer.getInitialization(v));
            }

            return String
                .format("new %s[]{%s}", value.getClass().getComponentType().getSimpleName(), sb.toString());
        }

        throw new IllegalStateException("One dim arrays is expected!");
    }
}
