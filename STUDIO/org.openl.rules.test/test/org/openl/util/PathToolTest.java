/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import org.junit.Test;

/**
 * @author snshor
 */
public class PathToolTest {

    @Test
    public void testMergePath() throws Exception {
        URL url = new File("tst/org/util/PathToolTest.java").toURI().toURL();

        String originalPath = url.getPath();
        String parent = "tst/org/";
        String expected = originalPath.substring(0,
            originalPath.lastIndexOf(parent) + parent.length()) + "include/XyZ.java";
        assertEquals(expected, PathTool.mergePath(originalPath, "./../include/XyZ.java"));

        assertEquals("../include/foo.xls", PathTool.mergePath("../include/", "foo.xls"));
        assertEquals("/some/path/bar.xls", PathTool.mergePath("/some/path/foo.xls", "bar.xls"));
    }

    @Test
    public void testSplitPath() {
        String[] res = PathTool.splitPath("../../");
        assertEquals(2, res.length);
    }

}
