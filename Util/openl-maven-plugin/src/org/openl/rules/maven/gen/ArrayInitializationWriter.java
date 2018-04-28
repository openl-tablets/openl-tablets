package org.openl.rules.maven.gen;

import java.lang.reflect.Array;

public class ArrayInitializationWriter implements TypeInitializationWriter {
    @Override
    public String getInitialization(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Not null value is expected!");
        }

        Class<?> type = value.getClass();
        if (!type.isArray()) {
            throw new IllegalStateException("Array type is expected!");
        }

        int length = Array.getLength(value);
        if (length == 0) {
            int dims = 0;
            while (type.isArray()) {
                dims++;
                type = type.getComponentType();
            }
            StringBuilder sb = new StringBuilder("new ").append(type.getSimpleName());
            while (dims > 0) {
                sb.append("[0]");
                dims--;
            }

            return sb.toString();
        } else if (!value.getClass().getComponentType().isArray()) {

            TypeInitializationWriter writer = SimpleBeanJavaGenerator
                .geTypeInitializationWriter(value.getClass().getComponentType());

            StringBuilder sb = new StringBuilder("new ").append(value.getClass().getComponentType().getSimpleName())
                .append("[] {");

            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Object v = Array.get(value, i);
                sb.append(writer.getInitialization(v));
            }

            return sb.append('}').toString();
        }

        throw new IllegalStateException("One dim arrays is expected!");
    }
}
