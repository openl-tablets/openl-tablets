package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatDomainImpl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigen Group, Inc.
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class TestFloatDomainImpl extends TestCase {
    private Constrainer C = new Constrainer("TestFloatDomainImpl");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatDomainImpl.class));
    }

    public TestFloatDomainImpl(String name) {
        super(name);
    }

    public void testContains() {
        FloatVar floatVar = C.addFloatVar(-2, 3, "");
        FloatDomainImpl floatDomain = new FloatDomainImpl(floatVar, floatVar.min(), floatVar.max());
        assertTrue(floatDomain.contains(-2 - Constrainer.precision()));
        assertTrue(!floatDomain.contains(-2 - Constrainer.precision() / 0.99998));
        assertTrue(floatDomain.contains(3 + Constrainer.precision()));
        assertTrue(!floatDomain.contains(3 + Constrainer.precision() / 0.99998));
    }

    public void testSetMax() {
        try {
            FloatVar floatVar = C.addFloatVar(-2, 3, "");
            FloatDomainImpl floatDomain = new FloatDomainImpl(floatVar, floatVar.min(), floatVar.max());
            assertTrue(!floatDomain.setMax(3 + Constrainer.precision()));

            try {
                floatVar.setMax(floatDomain.min() - 2 * Constrainer.precision());
                fail("test failed : setMax() allows max < min");
            } catch (Failure f) {/* that's ok */
            }

            assertTrue(floatDomain.setMax(-2 - Constrainer.precision()));

            assertEquals(-2, floatDomain.max(), 2 * Constrainer.precision());

        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSetMin() {
        try {
            FloatVar floatVar = C.addFloatVar(-2, 3, "");
            FloatDomainImpl floatDomain = new FloatDomainImpl(floatVar, floatVar.min(), floatVar.max());
            assertTrue(!floatDomain.setMin(-2 - Constrainer.precision()));

            try {
                floatVar.setMin(floatDomain.max() + 2 * Constrainer.precision());
                fail("test failed : setMax() allows max < min");
            } catch (Failure f) {/* that's ok */
            }

            assertTrue(floatDomain.setMin(3 + Constrainer.precision()));

            assertEquals(3, floatDomain.min(), 2 * Constrainer.precision());

        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSetValue() {
        try {
            FloatVar floatVar = C.addFloatVar(-2, 3, "");
            FloatDomainImpl floatDomain = new FloatDomainImpl(floatVar, floatVar.min(), floatVar.max());
            assertTrue(floatDomain.setValue(-2 - Constrainer.precision()));
            assertEquals(-2, floatDomain.min(), 2 * Constrainer.precision());
            assertEquals(-2, floatDomain.max(), 2 * Constrainer.precision());

            floatVar = C.addFloatVar(-2, 3, "");
            floatDomain = new FloatDomainImpl(floatVar, floatVar.min(), floatVar.max());
            assertTrue(floatDomain.setValue(3 + Constrainer.precision()));
            assertEquals(3, floatDomain.min(), 2 * Constrainer.precision());
            assertEquals(3, floatDomain.max(), 2 * Constrainer.precision());

            floatVar = C.addFloatVar(-2, 3, "");
            floatDomain = new FloatDomainImpl(floatVar, floatVar.min(), floatVar.max());
            assertTrue(floatDomain.setValue(0));
            assertEquals(0, floatDomain.min(), 2 * Constrainer.precision());
            assertEquals(0, floatDomain.max(), 2 * Constrainer.precision());

            floatVar = C.addFloatVar(-2, 3, "");
            floatDomain = new FloatDomainImpl(floatVar, floatVar.min(), floatVar.max());
            try {
                floatDomain.setValue(3 + 2 * Constrainer.precision());
                fail("test failed");
            } catch (Failure f) {
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

}