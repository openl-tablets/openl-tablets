package org.openl.binding.impl.cast;

import java.lang.reflect.Array;

import org.junit.Assert;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

public class CastFactoryTest {

    public CastFactoryTest() {
        factory = new CastFactory();
        factory.setMethodFactory(NullOpenClass.the);
    }

    CastFactory factory;

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
                Assert.assertEquals(0, Array.getLength(y[i][j]));
            }
        }
        x = new Integer[5][4][1][1];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 1; k++) {
                    for (int w = 0; w < 1; w++) {
                        x[i][j][k][w] = 31 + i * 101 ^ j >>> 3 + k * 721 - w * 13 ;
                    }
                }
            }
        }
        y = (int[][][][]) cast.convert(x);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 1; k++) {
                    for (int w = 0; w < 1; w++) {
                        Assert.assertEquals(x[i][j][k][w].intValue(), y[i][j][k][w]);
                    }
                }
            }
        }

        cast = factory.getCast(JavaOpenClass.getOpenClass(Object.class),
            JavaOpenClass.getOpenClass(Integer[][][][].class));
        Integer[][][][] z = (Integer[][][][]) cast.convert(x);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 1; k++) {
                    for (int w = 0; w < 1; w++) {
                        Assert.assertEquals(x[i][j][k][w], z[i][j][k][w]);
                    }
                }
            }
        }

        cast = factory.getCast(JavaOpenClass.getOpenClass(Object.class), JavaOpenClass.getOpenClass(int[][][][].class));
        y = (int[][][][]) cast.convert(x);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 1; k++) {
                    for (int w = 0; w < 1; w++) {
                        Assert.assertEquals(x[i][j][k][w].intValue(), y[i][j][k][w]);
                    }
                }
            }
        }
        try {
            cast = factory.getCast(JavaOpenClass.getOpenClass(Object.class),
                JavaOpenClass.getOpenClass(int[][][][][].class));
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

        Assert.assertNull(factory.getCast(JavaOpenClass.getOpenClass(Integer[][][][][].class),
            JavaOpenClass.getOpenClass(int[][][][].class)));
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

}
