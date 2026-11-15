package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.TestUtils;

/**
 * A condition whose expression gives a value of a wider type than its column, such as {@code map.get("key")} of type
 * {@code Object} for a column of {@code Boolean} values.
 */
class ConditionValueTypeTest {

    private static final String SRC = "test/rules/dt/ConditionValueTypes.xlsx";
    private static Rules rules;

    @BeforeAll
    static void init() {
        rules = TestUtils.create(SRC, Rules.class);
    }

    @Test
    void valueOfTheColumnTypeMatches() {
        assertEquals("yes", rules.byBoolean(map(true)));
        assertEquals("no", rules.byBoolean(map(false)));
        assertEquals("yes", rules.byPrimitive(map(true)));
        assertEquals("no", rules.byPrimitive(map(false)));
        assertEquals("two and a half", rules.byDouble(map(2.5)));
        assertEquals("A or B", rules.byArray(map("B")));
        assertEquals("C", rules.byArray(map("C")));
        assertEquals("other", rules.byArray(map("D")));
        assertEquals("no", rules.byElement(List.of(false)));
        assertEquals("yes", rules.byFormula(map(true)));
        assertEquals("no", rules.byFormula(map(false)));
        assertEquals("yes", rules.byInput(true));
        assertEquals("no", rules.byInput(false));
    }

    @Test
    void emptyValueMatchesNoValue() {
        assertEquals("other", rules.byBoolean(map(null)));
        assertEquals("other", rules.byPrimitive(map(null)));
        assertEquals("other", rules.byDouble(map(null)));
        assertEquals("other", rules.byArray(map(null)));
        assertEquals("other", rules.byElement(Arrays.asList((Object) null)));
        assertEquals("other", rules.byFormula(map(null)));
        assertEquals("other", rules.byInput(null));
    }

    @Test
    void valueIsConvertedByAnImplicitCast() {
        // The same type twice, another type, a type without a cast, then the previous type again.
        assertEquals("one", rules.byDouble(map(1)));
        assertEquals("one", rules.byDouble(map(1)));
        assertEquals("one", rules.byDouble(map(1L)));
        assertFails("Condition 'C1' of table 'byDouble' expects a value of type 'Double', but got 'true' of type"
                + " 'Boolean'.", () -> rules.byDouble(map(true)));
        assertEquals("one", rules.byDouble(map(1L)));
    }

    @Test
    void valueOfAnotherTypeFailsTheCall() {
        assertFails("Condition 'C1' of table 'byBoolean' expects a value of type 'Boolean', but got 'true' of type"
                + " 'String'.", () -> rules.byBoolean(map("true")));
        assertFails("Condition 'C1' of table 'byPrimitive' expects a value of type 'Boolean', but got '1' of type"
                + " 'Integer'.", () -> rules.byPrimitive(map(1)));
        assertFails("Condition 'C1' of table 'byDouble' expects a value of type 'Double', but got '1' of type"
                + " 'String'.", () -> rules.byDouble(map("1")));
        assertFails("Condition 'C1' of table 'byArray' expects a value of type 'String', but got '1' of type"
                + " 'Integer'.", () -> rules.byArray(map(1)));
        assertFails("Condition 'C1' of table 'byElement' expects a value of type 'Boolean', but got '1' of type"
                + " 'Integer'.", () -> rules.byElement(List.of(1)));
        assertFails("Condition 'C1' of table 'byFormula' expects a value of type 'Boolean', but got 'true' of type"
                + " 'String'.", () -> rules.byFormula(map("true")));
        assertFails("Condition 'C1' of table 'byInput' expects a value of type 'Boolean', but got 'true' of type"
                + " 'String'.", () -> rules.byInput("true"));
        assertFails("Condition 'C1' of table 'byDouble' expects a value of type 'Double', but got '{1,2}' of type"
                + " 'int[]'.", () -> rules.byDouble(map(new int[] {1, 2})));
    }

    private static void assertFails(String message, Executable call) {
        var e = assertThrows(OpenLRuntimeException.class, call);
        assertEquals(message, e.getMessage());
    }

    private static Map<String, Object> map(Object value) {
        return Collections.singletonMap("key", value);
    }

    public interface Rules {
        String byBoolean(Map<String, Object> map);

        String byPrimitive(Map<String, Object> map);

        String byDouble(Map<String, Object> map);

        String byArray(Map<String, Object> map);

        String byFormula(Map<String, Object> map);

        String byElement(List<Object> list);

        String byInput(Object flag);
    }
}
