/*
 * Created on Jul 7, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author snshor
 */
public class UtilTest extends TestCase {

    /**
     * Constructor for ArrayToolTest.
     *
     * @param name
     */
    public UtilTest(String name) {
        super(name);
    }

    public void testBrackets() {
        String s1 = "{}";

        String[] res = StringTool.openBrackets(s1, '{', '}', " ");

        Assert.assertEquals(1, res.length);
        Assert.assertEquals(0, res[0].length());

        s1 = "{xxx{}} {[ac} {ad{fdf{fd}fd}}";

        res = StringTool.openBrackets(s1, '{', '}', " ");

        Assert.assertEquals(3, res.length);
    }

    public void testDimensionOfArray() {
        String[] x = { "abc" };

        String[][] y = {};

        int[][] z = {};

        Assert.assertEquals(1, ArrayTool.dimensionOfArray(x, String.class));

        Assert.assertEquals(2, ArrayTool.dimensionOfArray(y, String.class));

        Assert.assertEquals(1, ArrayTool.dimensionOfArray(y, x.getClass()));

        Assert.assertEquals(-1, ArrayTool.dimensionOfArray(z, String.class));

    }
    
    public void testArrayContains() {
        String[] strMas = new String[]{"my", "yee", null, "hello"};
        Assert.assertTrue(ArrayTool.contains(strMas, null));
    }

}
