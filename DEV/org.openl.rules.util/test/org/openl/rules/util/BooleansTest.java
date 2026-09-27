package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BooleansTest {

    @Test
    void testAnd() {
        // Empty
        assertNull(Booleans.and(new boolean[]{}));
        assertNull(Booleans.and((boolean[]) null));

        // True
        assertTrue(Booleans.and(new boolean[]{true}));
        assertTrue(Booleans.and(new boolean[]{true, true}));
        assertTrue(Booleans.and(new boolean[]{true, true, true}));

        // False
        assertFalse(Booleans.and(new boolean[]{false}));
        assertFalse(Booleans.and(new boolean[]{false, false}));
        assertFalse(Booleans.and(new boolean[]{false, false, false}));

        // True and False
        assertFalse(Booleans.and(new boolean[]{false, true}));
        assertFalse(Booleans.and(new boolean[]{true, false}));
        assertFalse(Booleans.and(new boolean[]{false, false, true}));
        assertFalse(Booleans.and(new boolean[]{false, true, false}));
        assertFalse(Booleans.and(new boolean[]{false, true, true}));
        assertFalse(Booleans.and(new boolean[]{true, false, false}));
        assertFalse(Booleans.and(new boolean[]{true, false, true}));
        assertFalse(Booleans.and(new boolean[]{true, true, false}));
    }

    static Stream<Arguments> testAnd2() {
        return Stream.of(
                // Empty
                arguments(new Boolean[]{}, null),
                arguments((Boolean[]) null, null),
                arguments(new Boolean[]{null}, null),
                arguments(new Boolean[]{null, null}, null),

                // True
                arguments(new Boolean[]{true}, true),
                arguments(new Boolean[]{true, true}, true),
                arguments(new Boolean[]{true, true, true}, true),

                // False
                arguments(new Boolean[]{false}, false),
                arguments(new Boolean[]{false, false}, false),
                arguments(new Boolean[]{false, false, false}, false),

                // True and False
                arguments(new Boolean[]{false, true}, false),
                arguments(new Boolean[]{true, false}, false),
                arguments(new Boolean[]{false, false, true}, false),
                arguments(new Boolean[]{false, true, false}, false),
                arguments(new Boolean[]{false, true, true}, false),
                arguments(new Boolean[]{true, false, false}, false),
                arguments(new Boolean[]{true, false, true}, false),
                arguments(new Boolean[]{true, true, false}, false),

                // Null and True
                arguments(new Boolean[]{null, true}, null),
                arguments(new Boolean[]{true, null}, null),
                arguments(new Boolean[]{null, null, true}, null),
                arguments(new Boolean[]{null, true, null}, null),
                arguments(new Boolean[]{null, true, true}, null),
                arguments(new Boolean[]{true, null, null}, null),
                arguments(new Boolean[]{true, null, true}, null),
                arguments(new Boolean[]{true, true, null}, null),

                // Null and False
                arguments(new Boolean[]{null, false}, false),
                arguments(new Boolean[]{false, null}, false),
                arguments(new Boolean[]{null, null, false}, false),
                arguments(new Boolean[]{null, false, null}, false),
                arguments(new Boolean[]{null, false, false}, false),
                arguments(new Boolean[]{false, null, null}, false),
                arguments(new Boolean[]{false, null, false}, false),
                arguments(new Boolean[]{false, false, null}, false),

                // Null and True and False
                arguments(new Boolean[]{null, true, false}, false),
                arguments(new Boolean[]{null, false, true}, false),
                arguments(new Boolean[]{true, null, false}, false),
                arguments(new Boolean[]{true, false, null}, false),
                arguments(new Boolean[]{false, null, true}, false),
                arguments(new Boolean[]{false, true, null}, false));
    }

    @ParameterizedTest
    @MethodSource
    void testAnd2(Boolean[] values, Boolean expected) {
        assertEquals(expected, Booleans.and(values));
    }

    @Test
    void testOr() {
        // Empty
        assertNull(Booleans.or(new boolean[]{}));
        assertNull(Booleans.or((boolean[]) null));

        // True
        assertTrue(Booleans.or(new boolean[]{true}));
        assertTrue(Booleans.or(new boolean[]{true, true}));
        assertTrue(Booleans.or(new boolean[]{true, true, true}));

        // False
        assertFalse(Booleans.or(new boolean[]{false}));
        assertFalse(Booleans.or(new boolean[]{false, false}));
        assertFalse(Booleans.or(new boolean[]{false, false, false}));

        // True and False
        assertTrue(Booleans.or(new boolean[]{false, true}));
        assertTrue(Booleans.or(new boolean[]{true, false}));
        assertTrue(Booleans.or(new boolean[]{false, false, true}));
        assertTrue(Booleans.or(new boolean[]{false, true, false}));
        assertTrue(Booleans.or(new boolean[]{false, true, true}));
        assertTrue(Booleans.or(new boolean[]{true, false, false}));
        assertTrue(Booleans.or(new boolean[]{true, false, true}));
        assertTrue(Booleans.or(new boolean[]{true, true, false}));
    }

    static Stream<Arguments> testOr2() {
        return Stream.of(
                // Empty
                arguments(new Boolean[]{}, null),
                arguments((Boolean[]) null, null),
                arguments(new Boolean[]{null}, null),
                arguments(new Boolean[]{null, null}, null),

                // True
                arguments(new Boolean[]{true}, true),
                arguments(new Boolean[]{true, true}, true),
                arguments(new Boolean[]{true, true, true}, true),

                // False
                arguments(new Boolean[]{false}, false),
                arguments(new Boolean[]{false, false}, false),
                arguments(new Boolean[]{false, false, false}, false),

                // True and False
                arguments(new Boolean[]{false, true}, true),
                arguments(new Boolean[]{true, false}, true),
                arguments(new Boolean[]{false, false, true}, true),
                arguments(new Boolean[]{false, true, false}, true),
                arguments(new Boolean[]{false, true, true}, true),
                arguments(new Boolean[]{true, false, false}, true),
                arguments(new Boolean[]{true, false, true}, true),
                arguments(new Boolean[]{true, true, false}, true),

                // Null and True
                arguments(new Boolean[]{null, true}, true),
                arguments(new Boolean[]{true, null}, true),
                arguments(new Boolean[]{null, null, true}, true),
                arguments(new Boolean[]{null, true, null}, true),
                arguments(new Boolean[]{null, true, true}, true),
                arguments(new Boolean[]{true, null, null}, true),
                arguments(new Boolean[]{true, null, true}, true),
                arguments(new Boolean[]{true, true, null}, true),

                // Null and False
                arguments(new Boolean[]{null, false}, null),
                arguments(new Boolean[]{false, null}, null),
                arguments(new Boolean[]{null, null, false}, null),
                arguments(new Boolean[]{null, false, null}, null),
                arguments(new Boolean[]{null, false, false}, null),
                arguments(new Boolean[]{false, null, null}, null),
                arguments(new Boolean[]{false, null, false}, null),
                arguments(new Boolean[]{false, false, null}, null),

                // Null and True and False
                arguments(new Boolean[]{null, true, false}, true),
                arguments(new Boolean[]{null, false, true}, true),
                arguments(new Boolean[]{true, null, false}, true),
                arguments(new Boolean[]{true, false, null}, true),
                arguments(new Boolean[]{false, null, true}, true),
                arguments(new Boolean[]{false, true, null}, true));
    }

    @ParameterizedTest
    @MethodSource
    void testOr2(Boolean[] values, Boolean expected) {
        assertEquals(expected, Booleans.or(values));
    }

    @Test
    void testAllTrue() {
        // Empty
        assertFalse(Booleans.allTrue(new boolean[]{}));
        assertFalse(Booleans.allTrue((boolean[]) null));
        assertFalse(Booleans.allTrue(new Boolean[]{}));
        assertFalse(Booleans.allTrue((Boolean[]) null));
        assertFalse(Booleans.allTrue(new Boolean[]{null}));
        assertFalse(Booleans.allTrue(new Boolean[]{null, null}));

        // primitive
        assertFalse(Booleans.allTrue(new boolean[]{false}));
        assertTrue(Booleans.allTrue(new boolean[]{true}));
        assertFalse(Booleans.allTrue(new boolean[]{false, false}));
        assertFalse(Booleans.allTrue(new boolean[]{false, true}));
        assertFalse(Booleans.allTrue(new boolean[]{true, false}));
        assertTrue(Booleans.allTrue(new boolean[]{true, true}));

        // Object type
        assertFalse(Booleans.allTrue(new Boolean[]{false}));
        assertTrue(Booleans.allTrue(new Boolean[]{true}));
        assertFalse(Booleans.allTrue(new Boolean[]{false, false}));
        assertFalse(Booleans.allTrue(new Boolean[]{false, true}));
        assertFalse(Booleans.allTrue(new Boolean[]{true, false}));
        assertTrue(Booleans.allTrue(new Boolean[]{true, true}));

        // Null
        assertFalse(Booleans.allTrue(new Boolean[]{null, false}));
        assertFalse(Booleans.allTrue(new Boolean[]{null, true}));
        assertFalse(Booleans.allTrue(new Boolean[]{false, null}));
        assertFalse(Booleans.allTrue(new Boolean[]{true, null}));
    }

    @Test
    void testAllFalse() {
        // Empty
        assertFalse(Booleans.allFalse(new boolean[]{}));
        assertFalse(Booleans.allFalse((boolean[]) null));
        assertFalse(Booleans.allFalse(new Boolean[]{}));
        assertFalse(Booleans.allFalse((Boolean[]) null));
        assertFalse(Booleans.allFalse(new Boolean[]{null}));
        assertFalse(Booleans.allFalse(new Boolean[]{null, null}));

        // primitive
        assertTrue(Booleans.allFalse(new boolean[]{false}));
        assertFalse(Booleans.allFalse(new boolean[]{true}));
        assertTrue(Booleans.allFalse(new boolean[]{false, false}));
        assertFalse(Booleans.allFalse(new boolean[]{false, true}));
        assertFalse(Booleans.allFalse(new boolean[]{true, false}));
        assertFalse(Booleans.allFalse(new boolean[]{true, true}));

        // Object type
        assertTrue(Booleans.allFalse(new Boolean[]{false}));
        assertFalse(Booleans.allFalse(new Boolean[]{true}));
        assertTrue(Booleans.allFalse(new Boolean[]{false, false}));
        assertFalse(Booleans.allFalse(new Boolean[]{false, true}));
        assertFalse(Booleans.allFalse(new Boolean[]{true, false}));
        assertFalse(Booleans.allFalse(new Boolean[]{true, true}));

        // Null
        assertFalse(Booleans.allFalse(new Boolean[]{null, false}));
        assertFalse(Booleans.allFalse(new Boolean[]{null, true}));
        assertFalse(Booleans.allFalse(new Boolean[]{false, null}));
        assertFalse(Booleans.allFalse(new Boolean[]{true, null}));
    }

    @Test
    void testAnyTrue() {
        // Empty
        assertFalse(Booleans.anyTrue(new boolean[]{}));
        assertFalse(Booleans.anyTrue((boolean[]) null));
        assertFalse(Booleans.anyTrue(new Boolean[]{}));
        assertFalse(Booleans.anyTrue((Boolean[]) null));
        assertFalse(Booleans.anyTrue(new Boolean[]{null}));
        assertFalse(Booleans.anyTrue(new Boolean[]{null, null}));

        // primitive
        assertFalse(Booleans.anyTrue(new boolean[]{false}));
        assertTrue(Booleans.anyTrue(new boolean[]{true}));
        assertFalse(Booleans.anyTrue(new boolean[]{false, false}));
        assertTrue(Booleans.anyTrue(new boolean[]{false, true}));
        assertTrue(Booleans.anyTrue(new boolean[]{true, false}));
        assertTrue(Booleans.anyTrue(new boolean[]{true, true}));

        // Object type
        assertFalse(Booleans.anyTrue(new Boolean[]{false}));
        assertTrue(Booleans.anyTrue(new Boolean[]{true}));
        assertFalse(Booleans.anyTrue(new Boolean[]{false, false}));
        assertTrue(Booleans.anyTrue(new Boolean[]{false, true}));
        assertTrue(Booleans.anyTrue(new Boolean[]{true, false}));
        assertTrue(Booleans.anyTrue(new Boolean[]{true, true}));

        // Null
        assertFalse(Booleans.anyTrue(new Boolean[]{null, false}));
        assertTrue(Booleans.anyTrue(new Boolean[]{null, true}));
        assertFalse(Booleans.anyTrue(new Boolean[]{false, null}));
        assertTrue(Booleans.anyTrue(new Boolean[]{true, null}));
    }

    @Test
    void testAnyFalse() {
        // Empty
        assertFalse(Booleans.anyFalse(new boolean[]{}));
        assertFalse(Booleans.anyFalse((boolean[]) null));
        assertFalse(Booleans.anyFalse(new Boolean[]{}));
        assertFalse(Booleans.anyFalse((Boolean[]) null));
        assertFalse(Booleans.anyFalse(new Boolean[]{null}));
        assertFalse(Booleans.anyFalse(new Boolean[]{null, null}));

        // primitive
        assertTrue(Booleans.anyFalse(new boolean[]{false}));
        assertFalse(Booleans.anyFalse(new boolean[]{true}));
        assertTrue(Booleans.anyFalse(new boolean[]{false, false}));
        assertTrue(Booleans.anyFalse(new boolean[]{false, true}));
        assertTrue(Booleans.anyFalse(new boolean[]{true, false}));
        assertFalse(Booleans.anyFalse(new boolean[]{true, true}));

        // Object type
        assertTrue(Booleans.anyFalse(new Boolean[]{false}));
        assertFalse(Booleans.anyFalse(new Boolean[]{true}));
        assertTrue(Booleans.anyFalse(new Boolean[]{false, false}));
        assertTrue(Booleans.anyFalse(new Boolean[]{false, true}));
        assertTrue(Booleans.anyFalse(new Boolean[]{true, false}));
        assertFalse(Booleans.anyFalse(new Boolean[]{true, true}));

        // Null
        assertTrue(Booleans.anyFalse(new Boolean[]{null, false}));
        assertFalse(Booleans.anyFalse(new Boolean[]{null, true}));
        assertTrue(Booleans.anyFalse(new Boolean[]{false, null}));
        assertFalse(Booleans.anyFalse(new Boolean[]{true, null}));
    }

    @Test
    void toBooleanOfNull() {
        assertNull(Booleans.toBoolean((Byte) null));
        assertNull(Booleans.toBoolean((Short) null));
        assertNull(Booleans.toBoolean((Integer) null));
        assertNull(Booleans.toBoolean((Long) null));
        assertNull(Booleans.toBoolean((Character) null));
        assertNull(Booleans.toBoolean((String) null));
        assertNull(Booleans.toBoolean((BigInteger) null));
    }

    @Test
    void toBooleanOfUnknownValue() {
        assertNull(Booleans.toBoolean(""));
        assertNull(Booleans.toBoolean("foo"));
        assertNull(Booleans.toBoolean("y "));
        assertNull(Booleans.toBoolean("false "));
        assertNull(Booleans.toBoolean((Character) 'b'));
        assertNull(Booleans.toBoolean('b'));
        assertNull(Booleans.toBoolean((byte) 10));
        assertNull(Booleans.toBoolean((short) 10));
        assertNull(Booleans.toBoolean(10));
        assertNull(Booleans.toBoolean(10L));
        assertNull(Booleans.toBoolean((Byte) (byte) 10));
        assertNull(Booleans.toBoolean((Short) (short) 10));
        assertNull(Booleans.toBoolean((Integer) 10));
        assertNull(Booleans.toBoolean((Long) 10L));
        assertNull(Booleans.toBoolean(new BigInteger("10")));
        assertNull(Booleans.toBoolean((byte) -10));
        assertNull(Booleans.toBoolean((short) -10));
        assertNull(Booleans.toBoolean(-10));
        assertNull(Booleans.toBoolean(-10L));
        assertNull(Booleans.toBoolean((Byte) (byte) -10));
        assertNull(Booleans.toBoolean((Short) (short) -10));
        assertNull(Booleans.toBoolean((Integer) (-10)));
        assertNull(Booleans.toBoolean((Long) (-10L)));
        assertNull(Booleans.toBoolean(new BigInteger("-10")));
    }

    @Test
    void toBooleanOfTrueString() {
        assertEquals(Boolean.TRUE, Booleans.toBoolean("y"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("Y"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("1"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("yes"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("YES"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("yEs"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("true"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("TRUE"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("TrUe"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("on"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("ON"));
        assertEquals(Boolean.TRUE, Booleans.toBoolean("On"));
    }

    @Test
    void toBooleanOfTrueNumberOrCharacter() {
        assertEquals(Boolean.TRUE, Booleans.toBoolean((byte) 1));
        assertEquals(Boolean.TRUE, Booleans.toBoolean((short) 1));
        assertEquals(Boolean.TRUE, Booleans.toBoolean(1));
        assertEquals(Boolean.TRUE, Booleans.toBoolean(1L));
        assertEquals(Boolean.TRUE, Booleans.toBoolean('1'));
        assertEquals(Boolean.TRUE, Booleans.toBoolean((Byte) (byte) 1));
        assertEquals(Boolean.TRUE, Booleans.toBoolean((Short) (short) 1));
        assertEquals(Boolean.TRUE, Booleans.toBoolean((Integer) 1));
        assertEquals(Boolean.TRUE, Booleans.toBoolean((Long) 1L));
        assertEquals(Boolean.TRUE, Booleans.toBoolean(new BigInteger("1")));
        assertEquals(Boolean.TRUE, Booleans.toBoolean('1'));
        assertEquals(Boolean.TRUE, Booleans.toBoolean('Y'));
        assertEquals(Boolean.TRUE, Booleans.toBoolean('y'));
        assertEquals(Boolean.TRUE, Booleans.toBoolean((Character) '1'));
        assertEquals(Boolean.TRUE, Booleans.toBoolean((Character) 'y'));
        assertEquals(Boolean.TRUE, Booleans.toBoolean((Character) 'Y'));
    }

    @Test
    void toBooleanOfFalseString() {
        assertEquals(Boolean.FALSE, Booleans.toBoolean("n"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("N"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("0"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("no"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("NO"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("nO"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("false"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("FALSE"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("fAlSe"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("off"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("OFF"));
        assertEquals(Boolean.FALSE, Booleans.toBoolean("oFf"));
    }

    @Test
    void toBooleanOfFalseNumberOrCharacter() {
        assertEquals(Boolean.FALSE, Booleans.toBoolean((byte) 0));
        assertEquals(Boolean.FALSE, Booleans.toBoolean((short) 0));
        assertEquals(Boolean.FALSE, Booleans.toBoolean(0));
        assertEquals(Boolean.FALSE, Booleans.toBoolean(0L));
        assertEquals(Boolean.FALSE, Booleans.toBoolean('0'));
        assertEquals(Boolean.FALSE, Booleans.toBoolean((Byte) (byte) 0));
        assertEquals(Boolean.FALSE, Booleans.toBoolean((Short) (short) 0));
        assertEquals(Boolean.FALSE, Booleans.toBoolean((Integer) 0));
        assertEquals(Boolean.FALSE, Booleans.toBoolean((Long) 0L));
        assertEquals(Boolean.FALSE, Booleans.toBoolean(new BigInteger("0")));
        assertEquals(Boolean.FALSE, Booleans.toBoolean('0'));
        assertEquals(Boolean.FALSE, Booleans.toBoolean('n'));
        assertEquals(Boolean.FALSE, Booleans.toBoolean('N'));
        assertEquals(Boolean.FALSE, Booleans.toBoolean((Character) '0'));
        assertEquals(Boolean.FALSE, Booleans.toBoolean((Character) 'n'));
        assertEquals(Boolean.FALSE, Booleans.toBoolean((Character) 'N'));
    }
}
