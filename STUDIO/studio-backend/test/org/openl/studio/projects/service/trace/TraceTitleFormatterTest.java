package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.base.INamedThing;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * The detailed-title value formatting follows the classic trace: a simple value — or a collection of simple
 * values — reads as itself; a complex object reads as its type name, a complex collection as
 * {@code Collection of <element>}. The legacy trace never dumped a complex object's contents.
 */
class TraceTitleFormatterTest {

    /** A complex (non-simple, non-collection) type, to exercise the type-name branch. */
    private record Bean(int x) {
    }

    @Test
    void aSimpleValueReadsAsItself() {
        assertEquals("754299", TraceTitleFormatter.cellValue(JavaOpenClass.getOpenClass(Double.class), 754299.0));
        assertEquals("5", TraceTitleFormatter.cellValue(JavaOpenClass.getOpenClass(Integer.class), 5));
    }

    @Test
    void aNullValueReadsAsNull() {
        assertEquals("null", TraceTitleFormatter.cellValue(JavaOpenClass.getOpenClass(Double.class), null));
    }

    @Test
    void aComplexObjectReadsAsItsTypeName() {
        IOpenClass bean = JavaOpenClass.getOpenClass(Bean.class);
        // The type name, not the object's contents ({x=1}) or its hashcode.
        assertEquals(bean.getDisplayName(INamedThing.SHORT), TraceTitleFormatter.cellValue(bean, new Bean(1)));
    }

    @Test
    void aCollectionOfSimpleValuesIsFormatted() {
        IOpenClass doubles = JavaOpenClass.getOpenClass(Double[].class);
        assertEquals("{1,2}", TraceTitleFormatter.cellValue(doubles, new Double[]{1.0, 2.0}));
    }

    @Test
    void aCollectionOfComplexObjectsReadsAsItsElementTypeNotItsContents() {
        IOpenClass beans = JavaOpenClass.getOpenClass(Bean[].class);
        String element = JavaOpenClass.getOpenClass(Bean.class).getDisplayName(INamedThing.SHORT);
        assertEquals("Collection of " + element, TraceTitleFormatter.cellValue(beans, new Bean[]{new Bean(1)}));
        assertEquals("{}", TraceTitleFormatter.cellValue(beans, new Bean[0]));
    }
}
