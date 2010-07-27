package org.openl.ie.constrainer;

import java.util.Arrays;
import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntArray;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestIntArray extends TestCase {
    private Constrainer C = new Constrainer("test IntArray");
    private int[] intArray = { 0, 1, 2, 3, 4, 5, 6, 7 };

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntArray.class));
    }

    public TestIntArray(String name) {
        super(name);
    }

    public void testConstructors() {

        IntArray array1 = new IntArray(C, intArray);
        IntArray array2 = new IntArray(C, 8);
        Vector vec = new Vector(intArray.length);
        for (int i = 0; i < intArray.length; i++) {
            vec.add(i, new Integer(intArray[i]));
        }
        IntArray array3 = new IntArray(C, vec);
        IntArray array4 = new IntArray(C, 0, 1, 2, 3, 4, 5, 6, 7);
        assertTrue(Arrays.equals(array1.data(), array4.data()) && Arrays.equals(array1.data(), array3.data()));
    }

    public void testElementAt() {
        try {
            IntArray testArray = new IntArray(C, intArray);
            IntExpArray auxArray = new IntExpArray(C, testArray.size(), 0, 7, "array");
            IntExp sum = testArray.elementAt(auxArray.get(0));
            for (int i = 1; i < testArray.size(); i++) {
                sum = sum.add(testArray.elementAt(auxArray.get(i)));
            }
            C.postConstraint(sum.eq(28));
            C.postConstraint(C.allDiff(auxArray));
            Goal gen = new GoalGenerate(auxArray);
            boolean flag = C.execute(gen);
            assertTrue(flag);
            for (int i = 0; i < testArray.size(); i++) {
                assertEquals(auxArray.get(i).value(), testArray.elementAt(i));
            }
        } catch (Failure f) {
            f.printStackTrace();
        }
    }

    public void testWork() {
        int[][] A = { { -2, 9, 3 }, { 5, 7, 5 }, { 6, -3, -6 } };
        int[] b = { 50, 0, -120 };
        /*
         * Our aim is to find "indices" array subjected to: vars = {0, 10, -10};
         * x = {vars[indices[0]], vars[indices[1]], vars[indices[2]]}; Where x
         * is to satisfy the linear system: A*x = b( the only solution of which
         * is x = {-10, 0, 10}) The solution is: indices = {2, 0, 1};
         */
        try {
            int[] tmpArray = new int[b.length];
            int[] order = { 2, 0, 1 };
            for (int i = 0; i < b.length; i++) {
                tmpArray[order[i]] = -10 + i * 10;
            }
            IntArray vars = new IntArray(C, tmpArray);

            IntExpArray indices = new IntExpArray(C, b.length, 0, vars.size(), "cursor");

            IntExpArray x = new IntExpArray(C, vars.size());
            for (int i = 0; i < x.size(); i++) {
                x.set(vars.elementAt(indices.get(i)), i);
            }

            for (int i = 0; i < vars.size(); i++) {
                C.postConstraint(C.scalarProduct(x, A[i]).eq(b[i]));
            }

            Goal gen = new GoalGenerate(indices);
            boolean flag = C.execute(gen);

            assertTrue("!!!!!!!!!!!!!!!!", flag);
            for (int i = 0; i < indices.size(); i++) {
                assertEquals("indices[" + i + "] :", order[i], indices.get(i).value());
            }

        } catch (Failure f) {
            fail("test of IntExpArrayElement has failed with message: " + f.toString());
        }
    }

}
