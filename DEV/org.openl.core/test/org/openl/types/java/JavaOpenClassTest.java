package org.openl.types.java;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.meta.StringValue;
import org.openl.types.IOpenClass;

public class JavaOpenClassTest {

    @Test
    public void testGetComponentType() {
        assertEquals(JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String[].class).getComponentClass());

        assertEquals(JavaOpenClass.getOpenClass(String[].class), JavaOpenClass.getOpenClass(String[][].class).getComponentClass());

        assertNull(JavaOpenClass.getOpenClass(int.class).getComponentClass());

        assertEquals(JavaOpenClass.OBJECT, JavaOpenClass.getOpenClass(List.class).getComponentClass());
    }

    @Test
    public void testIsSimple() {
        IOpenClass clazz = JavaOpenClass.getOpenClass(StringValue.class);
        assertTrue(clazz.isSimple());
    }

    @Test
    public void testResetClassLoader() throws Exception {
        IOpenClass doubleValue = JavaOpenClass.getOpenClass(DoubleValue.class);
        IOpenClass myType = JavaOpenClass.getOpenClass(MyType.class);

        assertSame(doubleValue, JavaOpenClass.getOpenClass(DoubleValue.class));
        assertSame(myType, JavaOpenClass.getOpenClass(MyType.class));

        JavaOpenClass.resetClassloader(DoubleValue.class.getClassLoader());
        JavaOpenClass.resetClassloader(Exception.class.getClassLoader());

        assertSame(doubleValue, JavaOpenClass.getOpenClass(DoubleValue.class));
        assertNotSame(myType, JavaOpenClass.getOpenClass(MyType.class));
    }

    private static class MyType {}
}
