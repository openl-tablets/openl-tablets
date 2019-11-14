package org.openl.rules.helpers;

import static org.junit.Assert.*;

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
        assertNotNull(outer.getInnersNotEmpty()[0].getOuter());
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
        Inner[] innersNotEmpty = new Inner[] { new Inner() };

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

        public Inner[] getInnersNotEmpty() {
            return innersNotEmpty;
        }

        public void setInnersNotEmpty(Inner[] innersNotEmpty) {
            this.innersNotEmpty = innersNotEmpty;
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
