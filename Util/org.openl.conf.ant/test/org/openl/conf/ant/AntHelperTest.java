package org.openl.conf.ant;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.openl.conf.ConfigurableResourceContext;

import junit.framework.TestCase;

/*
 * Created on Nov 29, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author snshor
 */
public class AntHelperTest extends TestCase {

    /**
     * Constructor for AntHelperTest.
     *
     * @param name
     */
    public AntHelperTest(String name) {
        super(name);
    }

    public void testClassPathResources() {
        ConfigurableResourceContext cxt = new ConfigurableResourceContext(null);
        cxt.findClass("org.openl.conf.ant.Zzz");
        URL url = cxt.findClassPathResource("org/openl/conf/ant/Foo.properties");
        if (url != null) {
            System.out.println(url.toExternalForm());
        }
    }

    public void testJavaWrapper() throws URISyntaxException {
        Properties p = new Properties();
        URL url = this.getClass().getClassLoader().getResource("org/openl/conf/ant/TestJavaWrapper.build.xml");
        final URI uri = url.toURI();
        final String path = uri.getPath();
        new AntHelper(path, "aaa", p);
    }

    public void testProperties() throws URISyntaxException {
        Properties p = new Properties();
        p.put("zopa", "ZZopa");
        URL url = this.getClass().getClassLoader().getResource("org/openl/conf/ant/build.xml");
        new AntHelper(url.toURI().getPath(), "aaa", p);
    }
}
