package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.TestUtils;

/**
 * Conditions of SmartRules and SimpleRules tables on {@link Long} and {@link BigInteger} parameters.
 *
 * @author Yury Molchan
 */
class BigIntegerConditionsTest {

    private static Object instance;

    @BeforeAll
    static void init() {
        instance = TestUtils.create("test/rules/dt/BigIntegerConditions.xlsx");
    }

    private static Object call(String table, Object value) {
        return TestUtils.invoke(instance, table, new Class<?>[]{value.getClass()}, new Object[]{value});
    }

    @ParameterizedTest
    @ValueSource(strings = {"bigIntegerInRanges", "bigIntegerInRangesSimple"})
    void bigIntegerIsFoundInAListOfRanges(String table) {
        assertEquals("in ranges", call(table, BigInteger.valueOf(5)));
        assertEquals("beyond 2^53", call(table, new BigInteger("9007199254740993")));
        assertNull(call(table, BigInteger.valueOf(3)));
        // Beyond the long range, which bounds every IntRange.
        assertNull(call(table, new BigInteger("100000000000000000000")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"longInRanges", "longInRangesSimple"})
    void longIsFoundInAListOfRangesExactly(String table) {
        assertEquals("in ranges", call(table, 5L));
        assertEquals("beyond 2^53", call(table, 9007199254740993L));
        // The nearest double of 2^53 + 1 is 2^53, so comparing as double would match it too.
        assertNull(call(table, 9007199254740992L));
    }
}
