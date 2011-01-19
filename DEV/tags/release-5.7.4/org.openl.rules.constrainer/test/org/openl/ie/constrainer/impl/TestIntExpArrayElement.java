package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntExpArrayElement1;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


class TestData {
    static private class Data {
        int[][] A;
        int[] x;
        int[] b;
        int[] order;
    }

    static private java.util.HashMap _data = new java.util.HashMap();

    static private int _dim = 5;

    static private Data _curDat = (Data) _data.get(new Integer(_dim));

    static {
        Data dat1 = new Data();
        dat1.A = new int[][] { { 90, 53, 23, -19, -89 }, { -54, -9, 59, 88, -30 }, { 21, -97, 85, 84, 63 },
                { -3, 65, 48, -18, -99 }, { 79, -11, -65, 79, -73 } };
        dat1.x = new int[] { -9, -5, -1, 3, 7 };
        dat1.b = new int[] { -1778, 526, 904, -1093, -865 };
        dat1.order = new int[] { 3, 0, 4, 1, 2 };
        _data.put(new Integer(5), dat1);

        Data dat2 = new Data();
        dat2.A = new int[][] { { 17, -58, -17, -57, 37, -10, 22, -84, -76, -54 },
                { -15, -24, -39, 29, -58, -92, -97, -9, -10, -52 }, { 3, 57, 75, -36, 68, -95, -97, -12, 43, -90 },
                { -33, 36, -97, 92, 26, -38, -62, -29, 79, -85 }, { -13, -8, 54, 46, -74, -98, 17, -70, -46, 28 },
                { -55, 14, 95, -18, -59, -23, -89, 35, -49, -62 }, { 16, 59, 99, 49, 22, 37, -27, 40, 73, 69 },
                { 52, -89, 58, -47, 26, -82, 26, 46, -54, -66 }, { 6, 21, -12, -12, -26, -93, 44, -4, 61, -66 },
                { 28, -90, 0, 87, 15, 23, 39, 11, 82, 99 } };
        dat2.x = new int[] { -19, -15, -11, -7, -3, 1, 5, 9, 13, 17 };
        dat2.b = new int[] { -1540, -627, -3348, -595, -1092, -1751, -303, -1402, -373, 3230 };
        dat2.order = new int[] { 0, 2, 5, 3, 8, 6, 9, 4, 7, 1 };
        _data.put(new Integer(10), dat2);

        Data dat3 = new Data();
        dat3.A = new int[][] { { -87, -55, 57, -90, 99, 92, 37 }, { 98, 16, 36, -17, 58, 46, -58 },
                { 17, 52, -8, -39, -12, -18, 68 }, { -15, 6, 14, 75, 0, 49, 26 }, { 3, 28, 59, -97, -57, -47, -74 },
                { -33, -58, -89, 54, 29, -12, -59 }, { -13, -24, 21, 95, -36, 87, 22 } };
        dat3.x = new int[] { -12, -8, -4, 0, 4, 8, 12 };
        dat3.b = new int[] { 2832, -1544, 36, 780, -1988, 528, 1080 };
        dat3.order = new int[] { 0, 2, 1, 3, 6, 5, 4 };
        _data.put(new Integer(7), dat3);
    }

    static public int[][] getA() {
        return _curDat.A;
    }

    static public int[] getB() {
        return _curDat.b;
    }

    static public int getDim() {
        return _dim;
    }

    static public int[] getOrder() {
        return _curDat.order;
    }

    static public int[] getX() {
        return _curDat.x;
    }

    static public void init(int dim) {
        _dim = dim;
        _curDat = (Data) _data.get(new Integer(_dim));
    }

    private TestData() {
    }
}

public class TestIntExpArrayElement extends TestCase {

    private Constrainer C = new Constrainer("TestIntExpArrayElement");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpArrayElement.class));
    }

    public TestIntExpArrayElement(String name) {
        super(name);
    }

    private int max(int[] arr) {
        if ((arr == null) || (arr.length == 0)) {
            throw new IllegalArgumentException();
        }
        int max = -Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }
        return max;
    }

    private int min(int[] arr) {
        if ((arr == null) || (arr.length == 0)) {
            throw new IllegalArgumentException();
        }
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }
        return min;
    }

    public void testElementDomain(int stop) {
        int[] lb = { -10, -3, 45, -1, 92, 17, 10 };
        int[] ub = { -5, 21, 47, 15, 102, 31, 19 };
        IntExpArray array = new IntExpArray(C, lb.length);
        for (int i = 0; i < lb.length; i++) {
            array.set(C.addIntVar(lb[i], ub[i]), i);
        }
        IntVar index = C.addIntVar(2, lb.length - 1, IntVar.DOMAIN_BIT_FAST);
        IntExpArrayElement1 el = new IntExpArrayElement1(array, index);
        int[] domain = el.elementDomain();
        for (int i = 0; i < domain.length; i++) {
            System.out.println(domain[i] + ", ");
        }
        try {
            index.removeValue(3);
        } catch (Failure f) {
        }
        el = new IntExpArrayElement1(array, index);
        domain = el.elementDomain();
        for (int i = 0; i < domain.length; i++) {
            System.out.println(domain[i] + ", ");
        }
    }

    public void testMinAndMax(int stop) {
        try {
            int[] maxVals = { 125, 29, 12, 2, 4, 79, 110, 700 };
            int[] minVals = { -250, -100, -23, -5, 2, 30, -100, -250 };
            IntExpArray array = new IntExpArray(C, maxVals.length);
            for (int i = 0; i < array.size(); i++) {
                array.set(C.addIntVar(minVals[i], maxVals[i]), i);
            }

            for (int i = 0; i < array.size(); i++) {
                IntVar cursor = C.addIntVar(0, i);
                IntExpArrayElement1 arrayElem = new IntExpArrayElement1(array, cursor);

                int[] tempMax = new int[i + 1];
                System.arraycopy(maxVals, 0, tempMax, 0, tempMax.length);
                int[] tempMin = new int[tempMax.length];
                System.arraycopy(minVals, 0, tempMin, 0, tempMax.length);

                int expectedMax = max(tempMax);
                int expectedMin = min(tempMin);
                // assertEquals(expectedMax, arrayElem.calc_max());
                // assertEquals(expectedMin, arrayElem.calc_min());
                assertEquals(expectedMax, arrayElem.max());
                assertEquals(expectedMin, arrayElem.min());
            }

            for (int i = 0; i < array.size() / 2; i++) {
                IntVar cursor = C.addIntVar(0, array.size() - 1);
                IntExpArrayElement1 arrayElem = new IntExpArrayElement1(array, cursor);

                cursor.setMin(i);
                cursor.setMax(array.size() - i - 1); // subarray
                                                        // [i..size()-i-1] of
                                                        // array
                C.propagate();

                int[] tempMax = new int[array.size() - 2 * i];
                // subarray [i..size()-i-1] of maxVals
                System.arraycopy(maxVals, i, tempMax, 0, tempMax.length);
                int[] tempMin = new int[tempMax.length];
                // subarray [i..size()-i-1] of minVals
                System.arraycopy(minVals, i, tempMin, 0, tempMin.length);

                int expectedMax = max(tempMax);
                int expectedMin = min(tempMin);
                // assertEquals(expectedMax, arrayElem.calc_max());
                // assertEquals(expectedMin, arrayElem.calc_min());
                assertEquals(expectedMax, arrayElem.max());
                assertEquals(expectedMin, arrayElem.min());
            }

            IntExpArrayElement1 elem = new IntExpArrayElement1(new IntExpArray(C, C.addIntVar(0, 1)), C.addIntVar(0, 1));
        } catch (Failure f) {
        }
    } // end of testCalc_MaxCalc_Min()

    public void testWork() {
        TestData.init(5);
        int[][] A = TestData.getA();

        int[] x = TestData.getX();
        int[] b = TestData.getB();
        int[] order = TestData.getOrder();

        /* A*x' = b' */

        /*
         * Our aim is to find "indices" array such that: x = {vars[indices[0]],
         * vars[indices[1]], vars[indices[2]]}; Where x is to satisfy the linear
         * system: each variable has only one value being a component of a
         * solution vector x in it's domain, e.g. if the solution vector is {-2,
         * 1} the variables would be: var[0] [-4, 0] and var[1] [0, 3] The
         * solution is: indices = order;
         */
        try {
            // <init block>
            IntExpArray vars = new IntExpArray(C, b.length);
            int delta = 0;
            for (int i = 0; i < b.length - 1; i++) {
                delta = x[i + 1] - x[i] - 1;
                vars.set(C.addIntVar(x[i] - delta, x[i] + delta, IntVar.DOMAIN_BIT_FAST), order[i]);
            }
            vars.set(C.addIntVar(x[x.length - 1] - delta, x[x.length - 1] + delta, IntVar.DOMAIN_BIT_FAST),
                    order[b.length - 1]);
            System.out.println(vars);
            // ~<init block>

            IntExpArray indices = new IntExpArray(C, b.length, 0, vars.size(), "cursor");

            IntExpArray x1 = new IntExpArray(C, b.length);
            for (int i = 0; i < x1.size(); i++) {
                x1.set(vars.elementAt(indices.get(i)), i);
            }

            // C.trace(indices);
            for (int i = 0; i < vars.size(); i++) {
                C.postConstraint(C.scalarProduct(x1, A[i]).eq(b[i]));
            }

            System.out.println("befor: " + x1);

            Goal gen = new GoalAnd(new GoalGenerate(vars), new GoalGenerate(indices));
            long start = System.currentTimeMillis();
            boolean flag = C.execute(gen);
            C.out().println("time wasted: " + (System.currentTimeMillis() - start));

            assertTrue("!!!!!!!!!!!!!!!!", flag);
            System.out.println("vars:\n");
            System.out.println(vars);
            System.out.println("indices:\n");
            System.out.println(indices);
            System.out.println("x:\n");
            System.out.println(x1);
            for (int i = 0; i < indices.size(); i++) {
                assertEquals("indices[" + i + "] :", order[i], indices.get(i).value());
            }

        } catch (Failure f) {
            fail("test of IntExpArrayElement has failed with message: " + f.toString());
        }
    }

}
