package org.openl.binding.impl.cast;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

public class CastFactoryTest {

    private final CastFactory factory;

    public CastFactoryTest() {
        factory = new CastFactory();
        factory.setMethodFactory(NullOpenClass.the);
    }

    @Test
    public void testPrimitives() {
        javaCastTest(Integer.class, int.class);
        javaCastTest(int.class, Integer.class);
        javaCastTest(Boolean.class, boolean.class);
        javaCastTest(boolean.class, Boolean.class);
        javaCastTest(Void.class, void.class);
        javaCastTest(void.class, Void.class);
    }

    @Test
    public void typeToOneElementArrayCastTest() {
        javaCastTest(Integer.class, Integer[].class);
        javaCastTest(int.class, int[].class);
        javaCastTest(int.class, Integer[].class);
        javaCastTest(Boolean.class, Object[].class);
        javaCastTest(Double.class, DoubleValue[].class);
    }

    @Test
    public void arrayTest() {
        javaArrayCastTest(Integer[][].class, int[][].class);
        javaArrayCastTest(int[].class, Integer[].class);
        javaArrayCastTest(Boolean[][][].class, boolean[][][].class);
        javaArrayCastTest(boolean[][][][].class, Boolean[][][][].class);
        javaArrayCastTest(Object.class, Integer[][].class);
        javaArrayCastTest(Object[].class, Integer[][][].class);
        javaArrayCastTest(Integer[][][].class, Object[].class);
        javaArrayCastTest(int[].class, Object[].class);

        Integer[][][][] x = new Integer[5][4][0][1];
        IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(Integer[][][][].class),
            JavaOpenClass.getOpenClass(int[][][][].class));
        Assert.assertNotNull(cast);
        int[][][][] y = (int[][][][]) cast.convert(x);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(0, Array.getLength(y[i][j]));
            }
        }
        x = new Integer[5][4][1][1];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 1; k++) {
                    for (int w = 0; w < 1; w++) {
                        x[i][j][k][w] = 31 + i * 101 ^ j >>> 3 + k * 721 - w * 13;
                    }
                }
            }
        }
        y = (int[][][][]) cast.convert(x);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 1; k++) {
                    for (int w = 0; w < 1; w++) {
                        assertEquals(x[i][j][k][w].intValue(), y[i][j][k][w]);
                    }
                }
            }
        }

        cast = factory.getCast(JavaOpenClass.getOpenClass(Object.class),
            JavaOpenClass.getOpenClass(Integer[][][][].class));
        assertFalse(cast.isImplicit());
        Integer[][][][] z = (Integer[][][][]) cast.convert(x);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 1; k++) {
                    for (int w = 0; w < 1; w++) {
                        assertEquals(x[i][j][k][w], z[i][j][k][w]);
                    }
                }
            }
        }

        cast = factory.getCast(JavaOpenClass.getOpenClass(Object.class), JavaOpenClass.getOpenClass(int[][][][].class));
        assertFalse(cast.isImplicit());
        y = (int[][][][]) cast.convert(x);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 1; k++) {
                    for (int w = 0; w < 1; w++) {
                        assertEquals(x[i][j][k][w].intValue(), y[i][j][k][w]);
                    }
                }
            }
        }
        try {
            cast = factory.getCast(JavaOpenClass.getOpenClass(Object.class),
                JavaOpenClass.getOpenClass(int[][][][][].class));
            assertFalse(cast.isImplicit());
            y = (int[][][][]) cast.convert(x);
            Assert.fail("ClassCastException was expected!");
        } catch (ClassCastException e) {
        }
        try {
            cast = factory.getCast(JavaOpenClass.getOpenClass(Integer[][][][][].class),
                JavaOpenClass.getOpenClass(int[][][][][].class));
            y = (int[][][][]) cast.convert(x);
            Assert.fail("ClassCastException was expected!");
        } catch (ClassCastException e) {
        }

        assertNull(factory.getCast(JavaOpenClass.getOpenClass(Integer[][][][][].class),
            JavaOpenClass.getOpenClass(int[][][][].class)));
    }

    @Test
    public void interfacesDownCastTest() {
        // should allow downcast from Object -> List
        IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(Object.class),
            JavaOpenClass.getOpenClass(List.class));
        assertNotNull(cast);
        assertEquals(JavaDownCast.class, cast.getClass());

        // should allow downcast from Apple -> List
        cast = factory.getCast(JavaOpenClass.getOpenClass(Apple.class), JavaOpenClass.getOpenClass(List.class));
        assertNotNull(cast);
        assertEquals(JavaDownCast.class, cast.getClass());

        // should allow downcast from Number -> Integer when target class is final and implements this interface
        cast = factory.getCast(JavaOpenClass.getOpenClass(Number.class), JavaOpenClass.getOpenClass(Integer.class));
        assertNotNull(cast);
        assertEquals(JavaDownCast.class, cast.getClass());

        // should allow downcast from List -> Apple
        cast = factory.getCast(JavaOpenClass.getOpenClass(List.class), JavaOpenClass.getOpenClass(Apple.class));
        assertNotNull(cast);
        assertEquals(JavaDownCast.class, cast.getClass());

        // should allow downcast from Apple -> Fruit when target class extends source class
        cast = factory.getCast(JavaOpenClass.getOpenClass(Apple.class), JavaOpenClass.getOpenClass(Fruit.class));
        assertNotNull(cast);
        assertEquals(JavaDownCast.class, cast.getClass());

        cast = factory.getCast(JavaOpenClass.getOpenClass(Object[][].class),
            JavaOpenClass.getOpenClass(Apple[][].class));
        assertNotNull(cast);
        assertEquals(ArrayCast.class, cast.getClass());

        cast = factory.getCast(JavaOpenClass.getOpenClass(Object[].class), JavaOpenClass.getOpenClass(Apple[][].class));
        assertNotNull(cast);
        assertEquals(JavaDownCast.class, cast.getClass());
    }

    @Test
    public void interfacesUpCastTest() {
        // should allow upcast from List -> Object
        IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(List.class),
            JavaOpenClass.getOpenClass(Object.class));
        assertNotNull(cast);
        assertEquals(JavaUpCast.class, cast.getClass());

        // should allow upcast from AppleFinalList -> List
        cast = factory.getCast(JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(Number.class));
        assertNotNull(cast);
        assertEquals(JavaUpCast.class, cast.getClass());

        // should allow upcast from Fruit -> Apple
        cast = factory.getCast(JavaOpenClass.getOpenClass(Fruit.class), JavaOpenClass.getOpenClass(Apple.class));
        assertNotNull(cast);
        assertEquals(JavaUpCast.class, cast.getClass());

        cast = factory.getCast(JavaOpenClass.getOpenClass(Apple[][].class), JavaOpenClass.getOpenClass(Object[].class));
        assertNotNull(cast);
        assertEquals(JavaUpArrayCast.class, cast.getClass());
    }

    @Test
    public void shouldNotAllowCast() {
        // should not allow downcast from Integer -> Apple when source class is final
        IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(Integer.class),
            JavaOpenClass.getOpenClass(Apple.class));
        assertNull(cast);

        // should not allow downcast from List -> Integer when target class is final and doesn't implement source
        cast = factory.getCast(JavaOpenClass.getOpenClass(List.class), JavaOpenClass.getOpenClass(Integer.class));
        assertNull(cast);

        // should not allow downcast from Integer -> List
        cast = factory.getCast(JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(List.class));
        assertNull(cast);

        // should not allow downcast from String[][] -> List when source class is array
        cast = factory.getCast(JavaOpenClass.getOpenClass(String[][].class), JavaOpenClass.getOpenClass(List.class));
        assertNull(cast);

        cast = factory.getCast(JavaOpenClass.getOpenClass(Object[][].class), JavaOpenClass.getOpenClass(List.class));
        assertNull(cast);

        // should not allow downcast from Apple -> Dog
        cast = factory.getCast(JavaOpenClass.getOpenClass(Apple.class), JavaOpenClass.getOpenClass(Dog.class));
        assertNull(cast);

        // should not allow downcast from List -> String[][]
        cast = factory.getCast(JavaOpenClass.getOpenClass(List.class), JavaOpenClass.getOpenClass(String[][].class));
        assertNull(cast);

        cast = factory.getCast(JavaOpenClass.getOpenClass(List.class), JavaOpenClass.getOpenClass(Object[][].class));
        assertNull(cast);

        cast = factory.getCast(JavaOpenClass.getOpenClass(Apple[][].class),
            JavaOpenClass.getOpenClass(Integer[][].class));
        assertNull(cast);

        cast = factory.getCast(JavaOpenClass.getOpenClass(Apple[].class), JavaOpenClass.getOpenClass(Object[][].class));
        assertNull(cast);
    }

    void javaCastTest(Class<?> from, Class<?> to) {

        IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(from), JavaOpenClass.getOpenClass(to));
        Assert.assertNotNull(cast);
        Assert.assertTrue(cast.isImplicit());

    }

    void javaArrayCastTest(Class<?> from, Class<?> to) {

        IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(from), JavaOpenClass.getOpenClass(to));
        Assert.assertNotNull(cast);

    }

    private static class Apple {
    }

    private static class Fruit extends Apple {
    }

    private static class Dog {
    }

}
