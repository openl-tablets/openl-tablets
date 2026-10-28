package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ArrayElementsTest {

    private static final List<Class<?>> ELEMENT_TYPES = List.of(Object.class,
            String.class,
            Number.class,
            Integer.class,
            int[].class,
            double.class,
            int.class,
            long.class,
            boolean.class,
            float.class,
            short.class,
            byte.class,
            char.class);

    private static final List<Object> VALUES = Arrays.asList(null,
            "text",
            7,
            7L,
            7.5,
            7.5f,
            (short) 7,
            (byte) 7,
            'c',
            true,
            new int[]{7});

    static Stream<Arguments> elementTypesAndValues() {
        return ELEMENT_TYPES.stream().flatMap(type -> VALUES.stream().map(value -> Arguments.of(type, value)));
    }

    @ParameterizedTest
    @MethodSource("elementTypesAndValues")
    void setAndGetBehaveAsReflection(Class<?> elementType, Object value) {
        var expectedArray = Array.newInstance(elementType, 2);
        var actualArray = Array.newInstance(elementType, 2);

        var expected = outcome(() -> Array.set(expectedArray, 1, value));
        var actual = outcome(() -> ArrayElements.set(actualArray, 1, value));

        assertEquals(expected, actual);
        assertEquals(Array.get(expectedArray, 0), ArrayElements.get(actualArray, 0));
        assertEquals(Array.get(expectedArray, 1), ArrayElements.get(actualArray, 1));
    }

    @Test
    void getKeepsTheSameObjectElement() {
        var element = new int[]{1};
        assertSame(element, ArrayElements.get(new int[][]{element}, 0));
    }

    @Test
    void notAnArrayIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ArrayElements.get("text", 0));
        assertThrows(IllegalArgumentException.class, () -> ArrayElements.set("text", 0, 1));
    }

    @Test
    void nullArrayIsRejected() {
        assertThrows(NullPointerException.class, () -> ArrayElements.get(nullArray(), 0));
        assertThrows(NullPointerException.class, () -> ArrayElements.set(nullArray(), 0, 1));
    }

    @ParameterizedTest
    @MethodSource("elementTypesAndValues")
    void indexOutsideTheArrayIsRejected(Class<?> elementType, Object value) {
        var array = Array.newInstance(elementType, 1);
        var expected = outcome(() -> Array.set(array, 1, value));
        assertEquals(expected, outcome(() -> ArrayElements.set(array, 1, value)));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayElements.get(array, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayElements.get(array, -1));
    }

    /**
     * Gives the class of the exception the action throws, or {@code null} when it completes.
     */
    private static Class<?> outcome(Runnable action) {
        try {
            action.run();
            return null;
        } catch (RuntimeException e) {
            return e.getClass();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static Object nullArray() {
        return null;
    }
}
