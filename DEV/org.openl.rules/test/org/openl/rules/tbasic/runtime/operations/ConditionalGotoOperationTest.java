package org.openl.rules.tbasic.runtime.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.rules.tbasic.runtime.ReturnType;

class ConditionalGotoOperationTest {

    private final ConditionalGotoOperation operation = new ConditionalGotoOperation("label", true);

    @Test
    void jumpsWhenTheConditionIsTheExpectedOne() {
        var result = operation.execute(null, true);

        assertEquals(ReturnType.GOTO, result.getReturnType());
        assertEquals("label", result.getValue());
    }

    @Test
    void goesToTheNextOperationOtherwise() {
        assertEquals(ReturnType.NEXT, operation.execute(null, false).getReturnType());
    }

    @Test
    void needsABooleanCondition() {
        assertThrows(NullPointerException.class, () -> operation.execute(null, null));
        assertThrows(ClassCastException.class, () -> operation.execute(null, "true"));
    }
}
