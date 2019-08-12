package org.openl.rules.binding;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class ArraysInDataTableTest {
    private static final String SRC = "test/rules/binding/ArraysInDataTableTest.xlsx";

    private static ArraysInDataTableTestInterf instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create(SRC, ArraysInDataTableTestInterf.class);
    }

    @Test
    public void testIntArrays1() {
        assertArrayEquals(new int[][] { { 1, 2, 0, 3 }, { 2, 3, 0, 4 }, { 4, 5, 0, 6 } }, instance.getIntArrays1());
    }

    @Test
    public void testIntArrays2() {
        assertArrayEquals(new int[][] { { 1, 2, 3 }, { 2, 3, 4 }, { 4, 5, 6 } }, instance.getIntArrays2());
    }

    @Test
    public void testIntArrays3() {
        assertArrayEquals(new int[][] { { 1, 2, 3 }, { 2, 4, 5 }, { 3, 4, 6 } }, instance.getIntArrays3());
    }

    @Test
    public void testIntegerArrays1() {
        assertArrayEquals(new Integer[][] { { 1, 2, null, 3 }, { 2, 3, null, 4 }, { 4, 5, null, 6 } },
            instance.getIntegerArrays1());
    }

    @Test
    public void testIntegerArrays2() {
        assertArrayEquals(new Integer[][] { { 1, 2, 3 }, { 2, 3, 4 }, { 4, 5, 6 } }, instance.getIntegerArrays2());
    }

    @Test
    public void testIntegerArrays3() {
        assertArrayEquals(new Integer[][] { { 1, 2, 3 }, { 2, 4, 5 }, { 3, 4, 6 } }, instance.getIntegerArrays3());
    }

    @Test
    public void testStringArrays1() {
        assertArrayEquals(new String[][] { { "x", "y", "z" }, { "y", "z", "x" }, { "z", "x", "y" } },
            instance.getStringArrays1());
    }

    @Test
    public void testStringArrays2() {
        assertArrayEquals(new String[][] { { "x", "y", "z" }, { "y", "z", "x" }, { "z", "x", "y" } },
            instance.getStringArrays2());
    }

    @Test
    public void testStringArrays3() {
        assertArrayEquals(new String[][] { { "x", "y", "z" }, { "y", "z", "x" }, { "z", "x", "y" } },
            instance.getStringArrays3());
    }

    @Test
    public void testBeanAArrays1() {
        BeanA x = new BeanA();
        BeanA y = new BeanA();
        BeanA z = new BeanA();
        x.setName("x");
        y.setName("y");
        z.setName("z");

        assertArrayEquals(new BeanA[][] { { x, y, z }, { y, z, x }, { z, x, y } }, instance.getBeanAArrays1());
    }

    @Test
    public void testBeanAArrays2() {
        BeanA x = new BeanA();
        BeanA y = new BeanA();
        BeanA z = new BeanA();
        x.setName("x");
        y.setName("y");
        z.setName("z");

        BeanA[][] result = instance.getBeanAArrays2();
        assertArrayEquals(new BeanA[][] { { x, y, z }, { y, z, x }, { z, x, y } }, result);

        boolean[][] r = { { true, true }, { true, false, false, true }, { true, false, false, true }, };

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                BeanB[] beansB = result[i][j].getBeansB();
                for (int k = 0; k < beansB.length; k++) {
                    if (r[j][k]) {
                        assertNotNull(beansB[k]);
                    } else {
                        assertNull(beansB[k]);
                    }
                }
            }
        }
    }

    public interface ArraysInDataTableTestInterf {
        int[][] getIntArrays1();

        int[][] getIntArrays2();

        int[][] getIntArrays3();

        Integer[][] getIntegerArrays1();

        Integer[][] getIntegerArrays2();

        Integer[][] getIntegerArrays3();

        String[][] getStringArrays1();

        String[][] getStringArrays2();

        String[][] getStringArrays3();

        BeanA[][] getBeanAArrays1();

        BeanA[][] getBeanAArrays2();
    }

}
