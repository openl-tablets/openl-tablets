package org.openl.binding.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import org.openl.types.IOpenMethod;

class ConstructorUsageTest {

    private final IOpenMethod method = mock(IOpenMethod.class);
    private final ConstructorNode node = mock(ConstructorNode.class);

    @Test
    void usagesOfTheSameConstructorCallAreEqual() {
        var usage = new ConstructorUsage(node, 4, 10, method);
        var same = new ConstructorUsage(node, 4, 10, method);

        assertEquals(usage, same);
        assertEquals(usage.hashCode(), same.hashCode());
    }

    @Test
    void usagesOfDifferentConstructorCallsAreNotEqual() {
        var usage = new ConstructorUsage(node, 4, 10, method);

        assertNotEquals(usage, new ConstructorUsage(mock(ConstructorNode.class), 4, 10, method));
        assertNotEquals(usage, new ConstructorUsage(node, 5, 10, method));
    }

    @Test
    void constructorUsageIsNotEqualToMethodUsage() {
        var usage = new ConstructorUsage(node, 4, 10, method);

        assertNotEquals(usage, new MethodUsage(4, 10, method));
        assertNotEquals(new MethodUsage(4, 10, method), usage);
    }
}
