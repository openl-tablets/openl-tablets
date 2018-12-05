package org.openl.types.java;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.meta.StringValue;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class JavaOpenClassTest {

    @Test
    public void testGetComponentType() {
        assertEquals(JavaOpenClass.getOpenClass(String.class),
            JavaOpenClass.getOpenClass(String[].class).getComponentClass());

        assertEquals(JavaOpenClass.getOpenClass(String[].class),
            JavaOpenClass.getOpenClass(String[][].class).getComponentClass());

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

        JavaOpenClassCache.getInstance().resetClassloader(DoubleValue.class.getClassLoader());
        JavaOpenClassCache.getInstance().resetClassloader(Exception.class.getClassLoader());

        assertSame(doubleValue, JavaOpenClass.getOpenClass(DoubleValue.class));
        assertNotSame(myType, JavaOpenClass.getOpenClass(MyType.class));
    }

    @Test
    public void testGetMethod() {
        IOpenClass myType = JavaOpenClass.getOpenClass(MyType.class);
        assertNotNull(myType);
        IOpenMethod myTypeMethod1 = myType.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE });
        IOpenMethod myTypeMethod2 = myType.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Byte.class), JavaOpenClass.getOpenClass(Double.class) });
        IOpenMethod myTypeMethod3 = myType.getMethod("method1", new IOpenClass[] { JavaOpenClass.BYTE });

        assertNotNull(myTypeMethod1);
        assertNotNull(myTypeMethod2);
        assertNotNull(myTypeMethod3);

        assertNotEquals(myTypeMethod1, myTypeMethod2);
        assertNotEquals(myTypeMethod2, myTypeMethod3);
        assertNotEquals(myTypeMethod1, myTypeMethod3);

        assertSame(myTypeMethod1,
            myType.getMethod("method1", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }));
        assertSame(myTypeMethod2,
            myType.getMethod("method1",
                new IOpenClass[] { JavaOpenClass.getOpenClass(Byte.class), JavaOpenClass.getOpenClass(Double.class) }));
        assertSame(myTypeMethod3, myType.getMethod("method1", new IOpenClass[] { JavaOpenClass.BYTE }));

        IOpenClass extended = JavaOpenClass.getOpenClass(Extended.class);
        assertNotNull(extended);
        IOpenMethod extended1 = extended.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE });
        IOpenMethod extended2 = extended.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Byte.class), JavaOpenClass.getOpenClass(Double.class) });
        IOpenMethod extended3 = extended.getMethod("method1", new IOpenClass[] { JavaOpenClass.BYTE });

        assertNotNull(extended1);
        assertNotNull(extended2);
        assertNotNull(extended3);

        assertNotEquals(extended1, extended2);
        assertNotEquals(extended2, extended3);
        assertNotEquals(extended1, extended3);

        assertSame(extended1,
            extended.getMethod("method1", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }));
        assertSame(extended2,
            extended.getMethod("method1",
                new IOpenClass[] { JavaOpenClass.getOpenClass(Byte.class), JavaOpenClass.getOpenClass(Double.class) }));
        assertSame(extended3, extended.getMethod("method1", new IOpenClass[] { JavaOpenClass.BYTE }));

        assertNotEquals(myTypeMethod1, extended1);
        assertSame(myTypeMethod2, extended2);
        assertSame(myTypeMethod3, extended3);

        assertNull(myType.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.DOUBLE }));
        assertNull(myType.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.getOpenClass(Double.class) }));
        assertNull(myType.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(Double.class) }));
        assertNull(myType.getMethod("method1", new IOpenClass[] { JavaOpenClass.BYTE, JavaOpenClass.DOUBLE }));
        assertNull(myType.getMethod("method1", new IOpenClass[] { JavaOpenClass.getOpenClass(Byte.class) }));
        assertNull(myType.getMethod("method", new IOpenClass[] { JavaOpenClass.BYTE }));
        assertNull(myType.getMethod("method11", new IOpenClass[] { JavaOpenClass.BYTE }));

        assertNotNull(extended.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.DOUBLE }));
        assertNull(extended.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.getOpenClass(Double.class) }));
        assertNull(extended.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(Double.class) }));
        assertNull(extended.getMethod("method1", new IOpenClass[] { JavaOpenClass.BYTE, JavaOpenClass.DOUBLE }));
        assertNull(extended.getMethod("method1", new IOpenClass[] { JavaOpenClass.getOpenClass(Byte.class) }));
        assertNull(extended.getMethod("method", new IOpenClass[] { JavaOpenClass.BYTE }));
        assertNull(extended.getMethod("method11", new IOpenClass[] { JavaOpenClass.BYTE }));
    }

    @Test
    public void interfaceOpenClassMethodsTest() {
        IOpenClass openClass = JavaOpenClass.getOpenClass(MyInterface.class);
        IOpenMethod method1 = openClass.getMethod("method1",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE });
        assertNotNull(method1);
        for (Method method : Object.class.getMethods()) {
            IOpenClass[] params = new IOpenClass[method.getParameterTypes().length];
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                params[i] = JavaOpenClass.getOpenClass(method.getParameterTypes()[i]);
            }
            IOpenMethod m = openClass.getMethod(method.getName(), params);
            assertNotNull(m);
        }
    }

    public static class MyType {
        public void method1(int i, double j) {
        }

        public void method1(Byte i, Double j) {
        }

        public void method1(byte i) {
        }
    }

    public static class Extended extends MyType {
        public void method1(int i, double j) {
        }

        public void method1(Integer i, double j) {
        }
    }

    public static interface MyInterface {
        void method1(int i, double j);
    }
}
