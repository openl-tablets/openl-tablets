package org.openl.types.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.generated.packA.MyType;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

public class ComponentTypeArrayOpenClassTest {

    private IOpenClass from;
    private IOpenClass to;

    @BeforeEach
    public void setUp() {
        from = new ComponentTypeArrayOpenClass(new JavaOpenClass(MyType.class));
    }

    @Test
    public void testEquals_componentClassWithDiffPackages() {
        to = new ComponentTypeArrayOpenClass(new JavaOpenClass(org.openl.generated.packB.MyType.class));
        assertNotEquals(from, to);
    }

    @Test
    public void testEquals_componentClassWithSamePackages() {
        to = new ComponentTypeArrayOpenClass(new JavaOpenClass(MyType.class));
        assertEquals(from, to);
    }

    @Test
    public void testIsAssignableFromNullOpenClass() {
        to = new ComponentTypeArrayOpenClass(new JavaOpenClass(MyType.class));
        assertFalse(to.isAssignableFrom(NullOpenClass.the));
    }

    @Test
    public void test_toString() {
        assertEquals("[Lorg.openl.generated.packA.MyType;", from.toString());
    }

}
