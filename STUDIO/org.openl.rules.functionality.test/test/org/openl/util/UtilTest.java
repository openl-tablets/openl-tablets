/*
 * Created on Jul 7, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util;

import org.junit.Assert;
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

    public void testArrayContains() {
        String[] strMas = new String[]{"my", "yee", null, "hello"};
        Assert.assertTrue(ArrayTool.contains(strMas, null));
    }

}
