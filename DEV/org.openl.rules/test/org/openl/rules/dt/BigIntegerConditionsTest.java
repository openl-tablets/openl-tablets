package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.TestUtils;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenClass;

/**
 * Conditions of SmartRules and SimpleRules tables on {@link Long} and {@link BigInteger} parameters.
 *
 * @author Yury Molchan
 */
class BigIntegerConditionsTest {

    private static final String SRC = "test/rules/dt/BigIntegerConditions.xlsx";

    private static Object instance;
    private static IOpenClass module;

    @BeforeAll
    static void init() {
        var engineFactory = new RulesEngineFactory<>(SRC);
        module = engineFactory.getCompiledOpenClass().getOpenClass();
        instance = engineFactory.newEngineInstance();
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

    @ParameterizedTest
    @ValueSource(strings = {"bigIntegerNumbers", "bigIntegerNumbersSimple"})
    void bigIntegerIsFoundAmongNumericCells(String table) {
        assertEquals("int max", call(table, BigInteger.valueOf(Integer.MAX_VALUE)));
        assertEquals("beyond int", call(table, new BigInteger("2147483648")));
        assertEquals("beyond long", call(table, new BigInteger("100000000000000000000")));
        assertNull(call(table, new BigInteger("2147483649")));
        assertEquals(BigInteger.class, conditionType(table));
    }

    @ParameterizedTest
    @ValueSource(strings = {"longNumbers", "longNumbersSimple"})
    void longIsFoundAmongNumericCells(String table) {
        assertEquals("int max", call(table, (long) Integer.MAX_VALUE));
        assertEquals("beyond int", call(table, 2147483648L));
        assertNull(call(table, 2147483649L));
        assertEquals(Long.class, conditionType(table));
    }

    /**
     * Gives the type the values of the first condition of the table are read in.
     */
    private static Class<?> conditionType(String table) {
        var decisionTable = (DecisionTable) module.getMethods()
                .stream()
                .filter(method -> method.getName().equals(table))
                .findFirst()
                .orElseThrow();
        return decisionTable.getCondition(0).getParams()[0].getType().getInstanceClass();
    }
}
