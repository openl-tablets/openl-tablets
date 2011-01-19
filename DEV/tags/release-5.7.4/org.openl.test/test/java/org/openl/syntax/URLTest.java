package org.openl.syntax;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */

/**
 * @author snshor
 */
public class URLTest extends TestCase {
    public void testURL1() throws Exception {
        URL url = new File("tst/org/openl/syntax/impl/URlTest.java").toURL();
        System.out.println("File: = " + url.getFile());
        System.out.println("Path: = " + url.getPath());
        System.out.println("Host: = " + url.getHost());
        System.out.println("Prot: = " + url.getProtocol());
        System.out.println("Port: = " + url.getPort());

        URL u2 = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());

        System.out.println("Fil2: = " + u2.getFile());

        URL u3 = getClass().getClassLoader().getResource("java/lang/String.class");

        System.out.println("File: = " + u3.getFile());
        System.out.println("Path: = " + u3.getPath());
        System.out.println("Host: = " + u3.getHost());
        System.out.println("Prot: = " + u3.getProtocol());
        System.out.println("Port: = " + u3.getPort());

    }

}
