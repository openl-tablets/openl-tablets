package org.openl.rules.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.commons.lang3.StringUtils;
import org.openl.meta.*;

public final class RulesUtilsAdditional {

    private RulesUtilsAdditional() {
    }

    private static void fill(Object target, boolean notNullsForSimpleTypes, Deque<Class<?>> stack) {
        if (target == null) {
            return;
        }
        if (stack.contains(target.getClass())) {
            return; // Prevents stack overflow
        }
        stack.push(target.getClass());
        try {
            for (Field field : target.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(target);
                    if (fieldValue == null) {
                        Class<?> fieldType = field.getType();
                        if (fieldType.isArray()) {
                            fieldValue = Array.newInstance(fieldType.getComponentType(), 1);
                            Object elem = instantiateValue(fieldType.getComponentType(), notNullsForSimpleTypes);
                            elem = fill(elem, notNullsForSimpleTypes);
                            Array.set(fieldValue, 0, elem);
                        } else {
                            fieldValue = instantiateValue(field.getType(), notNullsForSimpleTypes);
                        }
                        field.set(target, fieldValue);
                        fill(fieldValue, notNullsForSimpleTypes, stack);
                    }
                } catch (IllegalAccessException | InstantiationException e) {
                }
            }
        } finally {
            stack.pop();
        }
    }

    private static Object instantiateValue(Class<?> fieldType,
            boolean notNullsForSimpleTypes) throws InstantiationException, IllegalAccessException {
        if (notNullsForSimpleTypes) {
            if (fieldType == Boolean.class) {
                return Boolean.FALSE;
            } else if (fieldType == Byte.class) {
                return (byte) 0;
            } else if (fieldType == Short.class) {
                return (short) 0;
            } else if (fieldType == Integer.class) {
                return (int) 0;
            } else if (fieldType == Long.class) {
                return (long) 0;
            } else if (fieldType == Float.class) {
                return (float) 0;
            } else if (fieldType == Double.class) {
                return (double) 0;
            } else if (fieldType == String.class) {
                return StringUtils.EMPTY;
            } else if (fieldType == BigInteger.class) {
                return BigInteger.ZERO;
            } else if (fieldType == BigDecimal.class) {
                return BigDecimal.ZERO;
            } else if (fieldType == ByteValue.class) {
                return new ByteValue((byte) 0);
            } else if (fieldType == ShortValue.class) {
                return new ShortValue((short) 0);
            } else if (fieldType == IntValue.class) {
                return new IntValue(0);
            } else if (fieldType == LongValue.class) {
                return new LongValue(0);
            } else if (fieldType == FloatValue.class) {
                return new FloatValue(0f);
            } else if (fieldType == DoubleValue.class) {
                return new DoubleValue(0d);
            } else if (fieldType == BigIntegerValue.class) {
                return BigIntegerValue.ZERO;
            } else if (fieldType == BigDecimalValue.class) {
                return BigDecimalValue.ZERO;
            }
        }
        return fieldType.newInstance();
    }

    public static <T> T fill(T value) {
        return fill(value, false);
    }

    public static <T> T fill(T value, boolean notNullsForSimpleTypes) {
        if (value == null) {
            return null;
        }
        fill(value, notNullsForSimpleTypes, new ArrayDeque<Class<?>>());
        return value;
    }
}
