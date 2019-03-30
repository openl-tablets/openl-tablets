package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////

/**
 * A helper for the integer arithmetic.
 */
final class IntCalc {
    /**
     * Searches the specified array of ints for the specified value using the binary search algorithm. The array
     * <strong>must</strong> be sorted (as by the <tt>sort</tt> method, above) prior to making this call. If it is not
     * sorted, the results are undefined. If the array contains multiple elements with the specified value, there is no
     * guarantee which one will be found.
     *
     * @param a the array to be searched.
     * @param key the value to be searched for.
     * @return index of the search key, if it is contained in the list; otherwise,
     *         <tt>(-(<i>insertion point</i>) - 1)</tt>. The <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the list: the index of the first element greater than the key, or
     *         <tt>list.size()</tt>, if all elements in the list are less than the specified key. Note that this
     *         guarantees that the return value will be &gt;= 0 if and only if the key is found.
     * @see #sort(int[])
     */
    public static int binarySearch(int[] a, int key) {
        return java.util.Arrays.binarySearch(a, key);

        // int low = 0;
        // int high = a.length-1;
        //
        // while (low <= high)
        // {
        // int mid =(low + high)/2;
        // int midVal = a[mid];
        //
        // if (midVal < key)
        // low = mid + 1;
        // else if (midVal > key)
        // high = mid - 1;
        // else
        // return mid; // key found
        // }
        // return -(low + 1); // key not found.
    }

    /**
     * Returns sorted v without duplicates.
     */
    public static int[] differentSortedValues(int[] v) {
        if (isDiffSorted(v)) {
            return v;
        }

        java.util.Arrays.sort(v);

        // remove duplicates
        int dst = 0;
        for (int src = 1; src < v.length; ++src) {
            if (v[src] != v[dst]) {
                v[++dst] = v[src];
            }
        }

        int size = dst + 1;

        if (size == v.length) {
            return v; // no duplicates
        }

        int[] v1 = new int[size];
        System.arraycopy(v, 0, v1, 0, size);
        return v1;
    }

    /**
     * Returns <code>x1 / x2</code> truncated towards negative infinity.
     */
    static public int divTruncToNegInf(int x1, int x2) {
        int v = x1 / x2; // v is (x1/x2) truncated towards zero

        // exact division -> no adjustment
        if (v * x2 == x1) {
            return v;
        }

        // the condition "exact division is > 0"
        boolean vIsPositive = ((x1 > 0 && x2 > 0) || (x1 < 0 && x2 < 0));

        // v is truncated to zero -> -inf when vIsPositive and +inf otherwise
        return vIsPositive ? v : v - 1;
    }

    /**
     * Returns <code>x1 / x2</code> truncated towards positive infinity.
     */
    static public int divTruncToPosInf(int x1, int x2) {
        int v = x1 / x2; // v is (x1/x2) truncated towards zero

        // exact division -> no adjustment
        if (v * x2 == x1) {
            return v;
        }

        // the condition "exact division is > 0"
        boolean vIsPositive = ((x1 > 0 && x2 > 0) || (x1 < 0 && x2 < 0));

        // v is truncated to zero -> -inf when vIsPositive and +inf otherwise
        return vIsPositive ? v + 1 : v;
    }

    /**
     * Returns <code>x1 / x2</code> truncated towards zero.
     */
    static public int divTruncToZero(int x1, int x2) {
        // According to JLS, "integer division in Java rounds towards zero".
        // Note: "rounds" should be changed to "truncates".
        return x1 / x2;
    }

    /**
     * Returns true if v is sorted and without duplicates.
     */
    static boolean isDiffSorted(int[] v) {
        for (int i = 0; i + 1 < v.length; ++i) {
            if (v[i] >= v[i + 1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code>.
     */
    public static int productMax(int min1, int max1, int min2, int max2) {
        // exp1 >= 0
        if (min1 >= 0) {
            return productMaxP(min1, max1, max2);
        } else if (max1 <= 0) {
            return productMaxN(min1, max1, min2);
        } else {
            // exp2 >= 0
            if (min2 >= 0) {
                return max1 * max2;
            } else if (max2 <= 0) {
                return min1 * min2;
            } else {
                return Math.max(min1 * min2, max1 * max2);
            }
        }
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code> where <code>[min1..max1] <= 0</code>.
     */
    public static int productMaxN(int min1, int max1, int min2) {
        if (min2 >= 0) {
            return max1 * min2;
        } else {
            return min1 * min2;
        }
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code> where <code>[min1..max1] >= 0</code>.
     */
    public static int productMaxP(int min1, int max1, int max2) {
        if (max2 >= 0) {
            return max1 * max2;
        } else {
            return min1 * max2;
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code>.
     */
    public static int productMin(int min1, int max1, int min2, int max2) {
        // exp1 >= 0
        if (min1 >= 0) {
            return productMinP(min1, max1, min2);
        } else if (max1 <= 0) {
            return productMinN(min1, max1, max2);
        } else {
            // exp2 >= 0
            if (min2 >= 0) {
                return min1 * max2;
            } else if (max2 <= 0) {
                return max1 * min2;
            } else {
                return Math.min(max1 * min2, min1 * max2);
            }
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code> where <code>[min1..max1] <= 0</code>.
     */
    public static int productMinN(int min1, int max1, int max2) {
        if (max2 >= 0) {
            return min1 * max2;
        } else {
            return max1 * max2;
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code> where <code>[min1..max1] >= 0</code>.
     */
    public static int productMinP(int min1, int max1, int min2) {
        if (min2 >= 0) {
            return min1 * min2;
        } else {
            return max1 * min2;
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>max([min1..max1]*[min2..max2]) <= max</code>.
     */
    static public void productSetMax(int max, IntExp exp1, IntExp exp2) throws Failure {
        int min1, max1;
        // exp1 >= 0
        if ((min1 = exp1.min()) >= 0) {
            productSetMaxP(max, min1, exp1.max(), exp2);
        }
        // exp1 <= 0
        else if ((max1 = exp1.max()) <= 0) {
            productSetMaxN(max, min1, max1, exp2);
        } else // exp1 changes sign
        {
            if (max < 0) {
                int m = divTruncToPosInf(max1, max);
                int M = divTruncToNegInf(min1, max);

                // adjust remove range for exact division
                if (m * max == max1) {
                    m++;
                }
                if (M * max == min1) {
                    M--;
                }

                if (m <= M) {
                    exp2.removeRange(m, M);
                }
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) <= max</code> where
     * <code>[min1..max1] <= 0</code>.
     */
    static public void productSetMaxN(int max, int min1, int max1, IntExp exp2) throws Failure {
        if (max == 0) {
            if (max1 < 0) {
                exp2.setMin(0);
            }
        } else {
            int v = max > 0 ? max1 : min1;
            if (v != 0) {
                // We have to adjust min -> truncate toward +inf
                int min2 = divTruncToPosInf(max, v);
                exp2.setMin(min2);
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) <= max</code> where
     * <code>[min1..max1] >= 0</code>.
     */
    static public void productSetMaxP(int max, int min1, int max1, IntExp exp2) throws Failure {
        if (max == 0) {
            if (min1 > 0) {
                exp2.setMax(0);
            }
        } else {
            int v = max > 0 ? min1 : max1;
            if (v != 0) {
                // We have to adjust max -> truncate toward -inf
                int max2 = divTruncToNegInf(max, v);
                exp2.setMax(max2);
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) >= min</code>.
     */
    static public void productSetMin(int min, IntExp exp1, IntExp exp2) throws Failure {
        int min1, max1;
        // exp1 >= 0
        if ((min1 = exp1.min()) >= 0) {
            productSetMinP(min, min1, exp1.max(), exp2);
        }
        // exp1 <= 0
        else if ((max1 = exp1.max()) <= 0) {
            productSetMinN(min, min1, max1, exp2);
        } else // exp1 changes sign
        {
            if (min > 0) {
                int m = divTruncToPosInf(min1, min);
                int M = divTruncToNegInf(max1, min);

                // adjust remove range for exact division
                if (m * min == min1) {
                    m++;
                }
                if (M * min == max1) {
                    M--;
                }

                if (m <= M) {
                    exp2.removeRange(m, M);
                }
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) >= min</code> where
     * <code>[min1..max1] <= 0</code>.
     */
    static public void productSetMinN(int min, int min1, int max1, IntExp exp2) throws Failure {
        if (min == 0) {
            if (max1 < 0) {
                exp2.setMax(0);
            }
        } else {
            int v = min > 0 ? min1 : max1;
            if (v != 0) {
                // We have to adjust max -> truncate toward -inf
                int max2 = divTruncToNegInf(min, v);
                exp2.setMax(max2);
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) >= min</code> where
     * <code>[min1..max1] >= 0</code>.
     */
    static public void productSetMinP(int min, int min1, int max1, IntExp exp2) throws Failure {
        if (min == 0) {
            if (min1 > 0) {
                exp2.setMin(0);
            }
        } else {
            int v = min > 0 ? max1 : min1;
            if (v != 0) {
                // We have to adjust min -> truncate toward +inf
                int min2 = divTruncToPosInf(min, v);
                exp2.setMin(min2);
            }
        }
    }

    /**
     * Returns the expression: <code>max(sqr(min),sqr(max))</code>.
     */
    static public int sqrMax(int min, int max) {
        return Math.max(min * min, max * max);
    }

    /**
     * Returns the expression: <code>min(sqr(min),sqr(max))</code>.
     */
    static public int sqrMin(int min, int max) {
        // min >= 0 && max >= 0
        if (min >= 0) {
            return min * min;
        }

        // min < 0 && max >= 0
        if (max >= 0) {
            return 0;
        }

        // min < 0 && max > 0
        return max * max;
    }

    /**
     * Returns sqrt(value) if value if a square, -1 otherwise.
     */
    static public int sqrtInt(int value) {
        int sqrtValue = (int) Math.sqrt(value);
        return sqrtValue * sqrtValue == value ? sqrtValue : -1;
    }

} // ~ IntCalc
