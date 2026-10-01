package org.openl.types.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

class ADynamicTest {

    @Test
    void testIsAssignableFromNullOpenClass() {
        var d = new DummyDynamicClass("test");
        assertFalse(d.isAssignableFrom(NullOpenClass.the));
    }

    @Test
    void testNoStaticFields() {
        assertTrue(new DummyDynamicClass("test").getStaticFields().isEmpty());
        assertTrue(NullOpenClass.the.getStaticFields().isEmpty());
    }
}

class DummyDynamicClass extends ADynamicClass {

    @Override
    public Object newInstance(IRuntimeEnv env) {
        return null;
    }

    public DummyDynamicClass(String name) {
        super(name, DynamicObject.class);
    }
}
