///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000, 2001
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
package org.openl.ie.constrainer;

import org.junit.Assert;
import org.openl.ie.constrainer.impl.IntExpMulExp;

import junit.framework.TestCase;

public class IntExpMulExpTest extends TestCase {

    Constrainer c;

    String[] _names = { "PP", "PN", "PX", "XX", };

    int[] _min1 = { 1, 1, 1, -1, };

    int[] _max1 = { 2, 2, 2, 2, };

    int[] _min2 = { 3, -4, -4, -4, };
    int[] _max2 = { 4, -3, 3, 3, };

    public IntExpMulExpTest(String name) {
        super(name);
    }

    /**
     * Tests that product's min,max is min,max of the domain(v1)*domain(v2).
     */
    void _testProduct(IntExp v1, IntExp v2, IntExp product) {
        int prodMin = Integer.MAX_VALUE;
        int prodMax = Integer.MIN_VALUE;

        for (int i = v1.min(); i <= v1.max(); i++) {
            if (!v1.contains(i)) {
                continue;
            }

            for (int j = v2.min(); j <= v2.max(); j++) {
                if (!v2.contains(j)) {
                    continue;
                }

                int v = i * j;
                if (prodMin > v) {
                    prodMin = v;
                }
                if (prodMax < v) {
                    prodMax = v;
                }
            }
        }
        Assert.assertEquals("min", prodMin, product.min());
        Assert.assertEquals("max", prodMax, product.max());
    }

    @Override
    protected void setUp() throws Exception {
        c = new Constrainer("IntExpMulExpTest");
    }

    public void testAll() {
        for (int i = 0; i < _names.length; ++i) {
            IntVar v1 = c.addIntVar(_min1[i], _max1[i]);
            IntVar v2 = c.addIntVar(_min2[i], _max2[i]);

            IntExp prod = new IntExpMulExp(v1, v2);
            _testProduct(v1, v2, prod);
        }
    }

}
