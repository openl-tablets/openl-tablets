package org.openl.types.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

public class ComponentTypeArrayOpenClassTest {

    private IOpenClass from;
    private IOpenClass to;

    @Before
    public void setUp() {
        from = new ComponentTypeArrayOpenClass(new JavaOpenClass(org.openl.generated.packA.MyType.class));
    }

    @Test
    public void testEquals_componentClassWithDiffPackages() {
        to = new ComponentTypeArrayOpenClass(new JavaOpenClass(org.openl.generated.packB.MyType.class));
        assertNotEquals(from, to);
    }

    @Test
    public void testEquals_componentClassWithSamePackages() {
        to = new ComponentTypeArrayOpenClass(new JavaOpenClass(org.openl.generated.packA.MyType.class));
        assertEquals(from, to);
    }

    @Test
    public void testIsAssignableFromNullOpenClass() {
        to = new ComponentTypeArrayOpenClass(new JavaOpenClass(org.openl.generated.packA.MyType.class));
        assertFalse(to.isAssignableFrom(NullOpenClass.the));
    }

    @Test
    public void test_toString() {
        assertEquals("[Lorg.openl.generated.packA.MyType;", from.toString());
    }

}
