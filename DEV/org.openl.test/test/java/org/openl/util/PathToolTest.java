/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.util;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 * @author snshor
 */
public class PathToolTest extends TestCase {

    public void testMergePath() throws Exception {
        URL url = new File("tst/org/util/PathToolTest.java").toURL();

        String res = PathTool.mergePath(url.getPath(), "./../include/XyZ.java");

        System.out.println(url.getPath());
        System.out.println(res);

        res = PathTool.mergePath("../include/", "foo.xls");

        System.out.println(res);

        // url = new URL("http://localhost:8080/1040EZ/gen.pdf");
        // System.out.println( url.openConnection().getContentType());
        // System.out.println( url.openConnection().getContentLength());
        //
        // url.openStream();

    }

    public void testSplitPath() {
        String[] res = PathTool.splitPath("../../");
        Assert.assertEquals(2, res.length);
    }

}
