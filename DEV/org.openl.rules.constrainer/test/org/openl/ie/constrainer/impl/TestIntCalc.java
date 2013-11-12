package org.openl.ie.constrainer.impl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.impl.IntCalc;

import junit.framework.TestCase;


public class TestIntCalc extends TestCase {
    public TestIntCalc(String name) {
        super(name);
    }

    /**
     * Searches the specified array of ints for the specified value using the
     * binary search algorithm. The array <strong>must</strong> be sorted (as
     * by the <tt>sort</tt> method, above) prior to making this call. If it is
     * not sorted, the results are undefined. If the array contains multiple
     * elements with the specified value, there is no guarantee which one will
     * be found.
     *
     * param a the array to be searched. param key the value to be searched for.
     * return index of the search key, if it is contained in the list;
     * otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>. The
     * <i>insertion point</i> is defined as the point at which the key would be
     * inserted into the list: the index of the first element greater than the
     * key, or <tt>list.size()</tt>, if all elements in the list are less
     * than the specified key. Note that this guarantees that the return value
     * will be &gt;= 0 if and only if the key is found. public static int
     * binarySearch(int[] a, int key)
     */
    public void testBinarySearch() {
        int array[] = { 0, 1, 2, 3, 4, 5 };
        for (int i = 0; i < array.length; i++) {
            int result = IntCalc.binarySearch(array, array[i]);
            assertEquals("binarySearch [" + i + "]", i, result);
        }
        int array_dup[] = { 0, 1, 3, 3, 3, 5 };
        int result = IntCalc.binarySearch(array_dup, 3);
        assertTrue("binarySearch [duplcate]", result >= 2 && result <= 4);
    }

    /**
     * Returns sorted v without duplicates. public static int[]
     * differentSortedValues(int[] v)
     */
    public void testDifferentSortedValues() {
        int values[][] = { { 0, 1, 2, 3, 4, 5 }, { 0, 1, 2, 3, 5, 4 }, { 1, 0, 2, 3, 4, 5 }, { 0, 1, 3, 2, 4, 5 },
                { 1, 1, 2, 3, 4, 5 }, { 0, 1, 2, 2, 4, 5 }, { 0, 1, 2, 3, 5, 5 }, { 0, 1 }, { 1, 0 }, { 1, 1 }, { 1 },
                {} };
        int results[][] = { { 0, 1, 2, 3, 4, 5 }, { 0, 1, 2, 3, 4, 5 }, { 0, 1, 2, 3, 4, 5 }, { 0, 1, 2, 3, 4, 5 },
                { 1, 2, 3, 4, 5 }, { 0, 1, 2, 4, 5 }, { 0, 1, 2, 3, 5 }, { 0, 1 }, { 0, 1 }, { 1 }, { 1 }, {} };
        for (int i = 0; i < values.length; i++) {
            int[] result = IntCalc.differentSortedValues(values[i]);
            assertEquals("differentSortedValues [length]", results[i].length, result.length);
            for (int j = 0; j < results[i].length; j++) {
                assertEquals("differentSortedValues [" + i + "] [" + j + "]", results[i][j], result[j]);
            }
        }
    }

    /**
     * TODO remove from CTR static public int divTruncToZero(int x1, int x2); is
     * not used in CTR public void divTruncToZero () {}
     */
    /*
     * Returns <code>x1 / x2</code> truncated towards negative infinity.
     * static public int divTruncToNegInf(int x1, int x2)
     */
    public void testDivTruncToNegInf() {
        int dividends[] = { 7, -7, 7, -7, 8, -8, 8, -8, 5, -5, 5, -5, 0, 0 };
        int divisors[] = { 3, 3, -3, -3, 3, 3, -3, -3, 2, 2, -2, -2, 2, -2 };
        int results[] = { 2, -3, -3, 2, 2, -3, -3, 2, 2, -3, -3, 2, 0, 0 };
        for (int i = 0; i < dividends.length; i++) {
            assertEquals("( " + dividends[i] + "/" + divisors[i] + " )", results[i], IntCalc.divTruncToNegInf(
                    dividends[i], divisors[i]));
        }
        int excp_dividends[] = { 1, 0, -1 };
        for (int i = 0; i < excp_dividends.length; i++) {
            try {
                IntCalc.divTruncToNegInf(excp_dividends[i], 0);
                fail("Should be exception");
            } catch (java.lang.ArithmeticException e) {
            } catch (Throwable e) {
                fail("Unexpected exception:" + e);
            }
        }
    }

    /**
     * Returns <code>x1 / x2</code> truncated towards positive infinity.
     * static public int divTruncToPosInf(int x1, int x2)
     */
    public void testDivTruncToPosInf() {
        int dividends[] = { 7, -7, 7, -7, 8, -8, 8, -8, 5, -5, 5, -5, 0, 0 };
        int divisors[] = { 3, 3, -3, -3, 3, 3, -3, -3, 2, 2, -2, -2, 2, -2 };
        int results[] = { 3, -2, -2, 3, 3, -2, -2, 3, 3, -2, -2, 3, 0, 0 };
        for (int i = 0; i < dividends.length; i++) {
            assertEquals("( " + dividends[i] + "/" + divisors[i] + " )", results[i], IntCalc.divTruncToPosInf(
                    dividends[i], divisors[i]));
        }
        int excp_dividends[] = { 1, 0, -1 };
        for (int i = 0; i < excp_dividends.length; i++) {
            try {
                IntCalc.divTruncToPosInf(excp_dividends[i], 0);
                fail("Should be exception");
            } catch (java.lang.ArithmeticException e) {
            } catch (Throwable e) {
                fail("Unexpected exception:" + e);
            }
        }
    }

    /**
     * Returns true if v is sorted and without duplicates. static boolean
     * isDiffSorted(int[] v)
     */
    public void testIsDiffSorted() {
        int values[][] = { { 0, 1, 2, 3, 4, 5 }, { 0, 1, 2, 3, 5, 4 }, { 1, 0, 2, 3, 4, 5 }, { 0, 1, 3, 2, 4, 5 },
                { 1, 1, 2, 3, 4, 5 }, { 0, 1, 2, 2, 4, 5 }, { 0, 1, 2, 3, 5, 5 }, { 0, 1 }, { 1, 0 }, { 1, 1 }, { 1 },
                {} };
        boolean results[] = { true, false, false, false, false, false, false, true, false, false, true, true };
        for (int i = 0; i < values.length; i++) {
            assertEquals("isDiffSorted [" + i + "]", results[i], IntCalc.isDiffSorted(values[i]));
        }
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code>
     * where <code>[min1..max1] >= 0</code>. public static int
     * productMaxP(int min1, int max1, int min2, int max2)
     */
    public void testProductMax() {
        int min1s[] = { 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, 2, 2, 2, 2, -2, -2, -2, -7, -7, -7 };
        int max1s[] = { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 7, 7, 7, 7, 7, 7, 7, -2, -2, -2 };
        int min2s[] = { 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 3, -5, -3, -5, 3, -3, -5, 3, -3, -5 };
        int max2s[] = { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 5, 3, 5, -3, 5, 5, -3, 5, 5, -3 };
        int results[] = { 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 35, 21, 35, -6, 35, 35, 10, -6, 21, 35 };
        for (int i = 0; i < min1s.length; i++) {
            assertEquals("( [" + min1s[i] + ".." + max1s[i] + "]*[" + min2s[i] + ".." + max2s[i] + "] )", results[i],
                    IntCalc.productMax(min1s[i], max1s[i], min2s[i], max2s[i]));
        }
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code>
     * where <code>[min1..max1] <= 0</code>. public static int
     * productMaxN(int min1, int max1, int min2)
     */
    public void testProductMaxN() {
        int min1s[] = { 0, -1, -1, -1, -1, -5, -5 };
        int max1s[] = { 0, 0, -1, 0, 0, -2, -2 };
        int min2s[] = { 0, 0, 0, 1, -1, 3, -3 };
        int results[] = { 0, 0, 0, 0, 1, -6, 15 };
        for (int i = 0; i < min1s.length; i++) {
            assertEquals("( min1: " + min1s[i] + ", max1: " + max1s[i] + ", min2: " + min2s[i] + " )", results[i],
                    IntCalc.productMaxN(min1s[i], max1s[i], min2s[i]));
        }
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code>
     * where <code>[min1..max1] >= 0</code>. public static int
     * productMaxP(int min1, int max1, int max2)
     */
    public void testProductMaxP() {
        int min1s[] = { 0, 0, 0, 1, 2, 2 };
        int max1s[] = { 0, 1, 1, 1, 5, 5 };
        int max2s[] = { 0, 0, 1, 0, 3, -3 };
        int results[] = { 0, 0, 1, 0, 15, -6 };
        for (int i = 0; i < min1s.length; i++) {
            assertEquals("( min1: " + min1s[i] + ", max1: " + max1s[i] + ", max2: " + max2s[i] + " )", results[i],
                    IntCalc.productMaxP(min1s[i], max1s[i], max2s[i]));
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code>.
     * public static int productMin(int min1, int max1, int min2, int max2)
     */
    public void testProductMin() {
        int min1s[] = { 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, 2, 2, 2, 2, -2, -2, -2, -7, -7, -7 };
        int max1s[] = { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 7, 7, 7, 7, 7, 7, 7, -2, -2, -2 };
        int min2s[] = { 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 0, 0, -1, -1, 3, -5, -3, -5, 3, -3, -5, 3, -3, -5 };
        int max2s[] = { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 5, 3, 5, -3, 5, 5, -3, 5, 5, -3 };
        int results[] = { 0, 0, 0, 0, 0, 0, -1, -1, 0, -1, 0, -1, 0, -1, -1, -1, 6, -35, -21, -35, -10, -21, -35, -35,
                -35, 6 };

        for (int i = 0; i < min1s.length; i++) {
            assertEquals("( [" + min1s[i] + ".." + max1s[i] + "]*[" + min2s[i] + ".." + max2s[i] + "] )", results[i],
                    IntCalc.productMin(min1s[i], max1s[i], min2s[i], max2s[i]));
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code>
     * where <code>[min1..max1] <= 0</code>. public static int
     * productMinN(int min1, int max1, int max2)
     */
    public void testProductMinN() {
        int min1s[] = { 0, -1, -1, -1, -1, -5, -5 };
        int max1s[] = { 0, 0, -1, 0, 0, -2, -2 };
        int min2s[] = { 0, 0, 0, 1, -1, 3, -3 };
        int results[] = { 0, 0, 0, -1, 0, -15, 6 };
        for (int i = 0; i < min1s.length; i++) {
            assertEquals("( min1: " + min1s[i] + ", max1: " + max1s[i] + ", min2: " + min2s[i] + " )", results[i],
                    IntCalc.productMinN(min1s[i], max1s[i], min2s[i]));
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code>
     * where <code>[min1..max1] >= 0</code>. public static int
     * productMinP(int min1, int max1, int min2)
     */
    public void testProductMinP() {
        int min1s[] = { 0, 0, 1, 0, 2, 2 };
        int min2s[] = { 0, 0, 0, 1, 3, -3 };
        int max1s[] = { 0, 1, 1, 1, 5, 5 };
        int results[] = { 0, 0, 0, 0, 6, -15 };
        for (int i = 0; i < min1s.length; i++) {
            assertEquals("( min1: " + min1s[i] + ", max1: " + max1s[i] + ", min2: " + min2s[i] + " )", results[i],
                    IntCalc.productMinP(min1s[i], max1s[i], min2s[i]));
        }
    }

    /**
     * Adjust the expression exp2 so that:
     * <code>max([min1..max1]*[min2..max2]) <= max</code>. static public void
     * productSetMax(int max, IntExp exp1, IntExp exp2)
     */
    public void testProductSetMax() throws Failure {
        int maxs[] = { 0, 0, 0, 0, 0, 0, 3, 3, 3, 3, 3, 3, -3, -3, -3, -3, -3 };
        int min1s[] = { 0, 0, 1, -2, -2, -2, 0, 0, 1, -2, -2, -2, 0, 1, -2, -2, -2 };
        int max1s[] = { 0, 2, 2, 0, -1, 2, 0, 2, 2, 0, -1, 2, 2, 2, 0, -1, 2 };

        int res_min2s[] = { -10, -10, -10, -10, 0, -10, -10, -10, -10, -10, -3, -10, -10, -10, 2, 2, -10 };
        int res_max2s[] = { 10, 10, 0, 10, 10, 10, 10, 10, 3, 10, 10, 10, -2, -2, 10, 10, 10 };

        for (int i = 0; i < maxs.length; i++) {
            IntExp exp1 = AllTests.C.addIntVar(min1s[i], max1s[i]);
            IntExp exp2 = AllTests.C.addIntVar(-10, 10);
            IntCalc.productSetMax(maxs[i], exp1, exp2);
            assertEquals("( [" + min1s[i] + ".." + max1s[i] + "] <= " + maxs[i] + "): MIN", res_min2s[i], exp2.min());
            assertEquals("( [" + min1s[i] + ".." + max1s[i] + "] <= " + maxs[i] + "): MAX", res_max2s[i], exp2.max());
            assertEquals(min1s[i], exp1.min());
            assertEquals(max1s[i], exp1.max());
        }
    }

    /**
     * Adjust the expression exp2 so that:
     * <code>min([min1..max1]*[min2..max2]) <= max</code> where
     * <code>[min1..max1] <= 0</code>. static public void productSetMaxN(int
     * max, int min1, int max1, IntExp exp2)
     */
    public void testProductSetMaxN() throws Failure {
        int mins[] = { 0, 0, 0, -1, -1, 1, 1, 1 };
        int min1s[] = { 0, -1, -2, -1, -2, 0, -1, -2 };
        int max1s[] = { 0, 0, -1, 0, -1, 0, 0, -1 };
        int result_mins[] = { -10, -10, 0, 1, 1, -10, -10, -1 };

        for (int i = 0; i < mins.length; i++) {
            IntExp result = AllTests.C.addIntVar(-10, 10);
            IntCalc.productSetMaxN(mins[i], min1s[i], max1s[i], result);
            assertEquals(result_mins[i], result.min());
            assertEquals(10, result.max());
        }
        /*
         * try { IntExp result = AllTests.C.addIntVar (-10, 10);
         * IntCalc.productSetMaxP (-1, 0, 0, result); fail ("Should be
         * exception"); } catch (Failure e){ } catch (Throwable e) { fail
         * ("Unexpected exception:" + e); }
         */
    }

    /**
     * Adjust the expression exp2 so that:
     * <code>min([min1..max1]*[min2..max2]) <= max</code> where
     * <code>[min1..max1] >= 0</code>. static public void productSetMaxP(int
     * max, int min1, int max1, IntExp exp2)
     */
    public void testProductSetMaxP() throws Failure {
        int maxs[] = { 0, 0, 0, -1, -1, 1, 1, 1 };
        int min1s[] = { 0, 0, 1, 0, 1, 0, 0, 1 };
        int max1s[] = { 0, 1, 2, 1, 2, 0, 1, 2 };
        int result_maxs[] = { 10, 10, 0, -1, -1, 10, 10, 1 };

        for (int i = 0; i < maxs.length; i++) {
            IntExp result = AllTests.C.addIntVar(-10, 10);
            IntCalc.productSetMaxP(maxs[i], min1s[i], max1s[i], result);
            assertEquals(result_maxs[i], result.max());
            assertEquals(-10, result.min());
        }
        /*
         * try { IntExp result = AllTests.C.addIntVar (-10, 10);
         * IntCalc.productSetMaxP (-1, 0, 0, result); fail ("Should be
         * exception"); } catch (Failure e){ } catch (Throwable e) { fail
         * ("Unexpected exception:" + e); }
         */
    }

    /**
     * Adjust the expression exp2 so that:
     * <code>min([min1..max1]*[min2..max2]) >= min</code>. static public void
     * productSetMin(int min, IntExp exp1, IntExp exp2)
     */
    public void testProductSetMin() throws Failure {
        int mins[] = { 0, 0, 0, 0, 0, 0, 3, 3, 3, 3, 3, -3, -3, -3, -3, -3, -3 };
        int min1s[] = { 0, 0, 1, -2, -2, -2, 0, 1, -2, -2, -2, 0, 0, 1, -2, -2, -2 };
        int max1s[] = { 0, 2, 2, 0, -1, 2, 2, 2, 0, -1, 2, 0, 2, 2, 0, -1, 2 };

        int res_min2s[] = { -10, -10, 0, -10, -10, -10, 2, 2, -10, -10, -10, -10, -10, -3, -10, -10, -10 };
        int res_max2s[] = { 10, 10, 10, 10, 0, 10, 10, 10, -2, -2, 10, 10, 10, 10, 10, 3, 10 };

        for (int i = 0; i < mins.length; i++) {
            IntExp exp1 = AllTests.C.addIntVar(min1s[i], max1s[i]);
            IntExp exp2 = AllTests.C.addIntVar(-10, 10);
            IntCalc.productSetMin(mins[i], exp1, exp2);
            assertEquals("( [" + min1s[i] + ".." + max1s[i] + "] <= " + mins[i] + "): MIN", res_min2s[i], exp2.min());
            assertEquals("( [" + min1s[i] + ".." + max1s[i] + "] <= " + mins[i] + "): MAX", res_max2s[i], exp2.max());
            assertEquals(min1s[i], exp1.min());
            assertEquals(max1s[i], exp1.max());
        }
    }

    /**
     * Adjust the expression exp2 so that:
     * <code>min([min1..max1]*[min2..max2]) >= min</code> where
     * <code>[min1..max1] <= 0</code>. static public void productSetMinN(int
     * min, int min1, int max1, IntExp exp2)
     */
    public void testProductSetMinN() throws Failure {
        int mins[] = { 0, 0, 0, -1, -1, -1, 1, 1 };
        int min1s[] = { 0, -1, -2, 0, -1, -2, -1, -2 };
        int max1s[] = { 0, 0, -1, 0, 0, -1, 0, -1 };
        int result_maxs[] = { 10, 10, 0, 10, 10, 1, -1, -1 };

        for (int i = 0; i < mins.length; i++) {
            IntExp result = AllTests.C.addIntVar(-10, 10);
            IntCalc.productSetMinN(mins[i], min1s[i], max1s[i], result);
            assertEquals(result_maxs[i], result.max());
            assertEquals(-10, result.min());
        }
        /*
         * try { IntExp result = AllTests.C.addIntVar (-10, 10);
         * IntCalc.productSetMinN (1, 0, 0, result); fail ("Should be
         * exception"); } catch (Failure e){ } catch (Throwable e) { fail
         * ("Unexpected exception:" + e); }
         */
    }

    /**
     * Adjust the expression exp2 so that:
     * <code>min([min1..max1]*[min2..max2]) >= min</code> where
     * <code>[min1..max1] >= 0</code>. static public void productSetMinP (int
     * min, int min1, int max1, IntExp exp2) throws Failure
     */
    public void testProductSetMinP() throws Failure {
        int mins[] = { 0, 0, 0, -1, -1, -1, 1, 1 };
        int min1s[] = { 0, 0, 1, 0, 0, 1, 0, 1 };
        int max1s[] = { 0, 1, 2, 0, 1, 2, 1, 2 };
        int result_mins[] = { -10, -10, 0, -10, -10, -1, 1, 1 };

        for (int i = 0; i < mins.length; i++) {
            IntExp result = AllTests.C.addIntVar(-10, 10);
            IntCalc.productSetMinP(mins[i], min1s[i], max1s[i], result);
            assertEquals(result_mins[i], result.min());
            assertEquals(10, result.max());
        }
        /*
         * try { IntExp result = AllTests.C.addIntVar (-10, 10);
         * IntCalc.productSetMinP (1, 0, 0, result); fail ("Should be
         * exception"); } catch (Failure e){ } catch (Throwable e) { fail
         * ("Unexpected exception:" + e); }
         */
    }

    /**
     * Returns the expression: <code>max(sqr(min),sqr(max))</code>. static
     * public int sqrMax(int min, int max)
     */
    public void testSqrMax() {
        int mins[] = { 0, 0, 1, -2, -2 };
        int maxs[] = { 0, 2, 2, 0, 1 };
        int results[] = { 0, 4, 4, 4, 4 };
        for (int i = 0; i < maxs.length; i++) {
            assertEquals("SqrMax [" + mins[i] + ".." + maxs[i] + "]", results[i], IntCalc.sqrMax(mins[i], maxs[i]));
        }
    }

    /**
     * Returns the expression: <code>min(sqr(min),sqr(max))</code>. static
     * public int sqrMin(int min, int max)
     */
    public void testSqrMin() {
        int mins[] = { 0, 0, 1, -2, -2 };
        int maxs[] = { 0, 2, 2, 0, 1 };
        int results[] = { 0, 0, 1, 0, 0 };
        for (int i = 0; i < maxs.length; i++) {
            assertEquals("SqrMin [" + mins[i] + ".." + maxs[i] + "]", results[i], IntCalc.sqrMin(mins[i], maxs[i]));
        }
    }

    /**
     * Returns sqrt(value) if value if a square, -1 otherwise. static public int
     * sqrtInt(int value)
     */
    public void testSqrtInt() {
        int values[] = { 0, 1, 2, 9, -1, -2 };
        int results[] = { 0, 1, -1, 3, -1, -1 };
        for (int i = 0; i < values.length; i++) {
            assertEquals("SqrtInt [" + values[i] + "]", results[i], IntCalc.sqrtInt(values[i]));
        }
    }

}