package org.openl.rules.helpers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RulesUtilsAdditionalTest {

    @Test
    public void testSimple() {
        Outer outer = new Outer();
        RulesUtilsAdditional.fill(outer);
        assertNotNull(outer.getInner());
        assertNull(outer.getIntValue());
        assertNull(outer.getInner().getIntValue());
        assertNotNull(outer.getInners());
        assertEquals(1, outer.getInners().length);
        assertNotNull(outer.getInners()[0]);
    }

    @Test
    public void testNotNulls() {
        Outer outer = new Outer();
        RulesUtilsAdditional.fill(outer, true);
        assertNotNull(outer.getInner());
        assertNotNull(outer.getInner().getIntValue());
        assertNotNull(outer.getIntValue());
    }

    @Test
    public void nullTest() {
        assertNull(RulesUtilsAdditional.fill(null));
    }

    public static class Outer {
        Inner inner;
        String stringValue;
        Integer intValue;
        Inner[] inners;

        public Inner getInner() {
            return inner;
        }

        public void setInner(Inner inner) {
            this.inner = inner;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public Integer getIntValue() {
            return intValue;
        }

        public void setIntValue(Integer intValue) {
            this.intValue = intValue;
        }

        public Inner[] getInners() {
            return inners;
        }

        public void setInners(Inner[] inners) {
            this.inners = inners;
        }

    }

    public static class Inner {
        Integer intValue;
        Outer outer;

        public Integer getIntValue() {
            return intValue;
        }

        public void setIntValue(Integer intValue) {
            this.intValue = intValue;
        }

        public Outer getOuter() {
            return outer;
        }

        public void setOuter(Outer outer) {
            this.outer = outer;
        }
    }
}
