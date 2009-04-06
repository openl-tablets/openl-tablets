package org.openl.conf.ant;
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
public class AntHelperTest extends TestCase
{

	/**
	 * Constructor for AntHelperTest.
	 * @param name
	 */
	public AntHelperTest(String name)
	{
		super(name);
	}


	public void testProperties()
	{
		Properties p = new Properties();
		p.put("zopa", "ZZopa");
	  new AntHelper("tst.org.openl.conf.ant/org/openl/conf/ant/build.xml", "aaa", p);	
	}
	
	public void testJavaWrapper()
	{
		Properties p = new Properties();
	  new AntHelper("tst.org.openl.conf.ant/TestJavaWrapper.build.xml", "aaa", p);	
	}
	
	
	public void testClassPathResources()
	{
		ConfigurableResourceContext cxt = new ConfigurableResourceContext(null);
		cxt.findClass("org.openl.conf.ant.Zzz");
		URL url = cxt.findClassPathResource("org/openl/conf/ant/Foo.properties");
		if (url != null)
		  System.out.println(url.toExternalForm());
	}
}
