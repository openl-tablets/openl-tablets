package org.openl.binding;

import org.openl.OpenL;
import org.openl.syntax.impl.StringSourceCodeModule;

import junit.framework.Assert;
import junit.framework.TestCase;

public class RunTest extends TestCase
{
	void _runNoError(String expr, Object expected, String openl)
	{
		OpenL op = OpenL.getInstance(openl);
		Object res = op.evaluate(new StringSourceCodeModule(expr, null));
		Assert.assertEquals(expected, res);
	}

	public void testRun()
	{

		_runNoError("String x=null; x == null || x.length() < 10", true, "org.openl.j");
		_runNoError("String x=null; x != null && x.length() < 10", false, "org.openl.j");

		_runNoError("String x=null; Boolean b = true; b || x.length() < 10", true, "org.openl.j");
		_runNoError("String x=null; Boolean b = false; b && x.length() < 10", false, "org.openl.j");

		_runNoError("String x=\"abc\"; x == null || x.length() < 10", true, "org.openl.j");
		_runNoError("String x=\"abc\"; x != null && x.length() < 10", true, "org.openl.j");
		
		_runNoError("int x = 5; x += 4", new Integer(9), "org.openl.j");
		_runNoError("DoubleValue d1 = new DoubleValue(5); DoubleValue d2 = new DoubleValue(4); d1 += d2; d1.getValue()", new Double(9), "org.openl.rules.java");
		_runNoError("int i=0; for(int j=0; j < 10; ) {i += j;j++;} i", new Integer(45), "org.openl.j");

		//Testing new implementation of s1 == s2 for Strings. To achieve old identity test Strings must be downcasted to Object	
		_runNoError("String a=\"a\"; String b = \"b\"; a + b == a + 'b'", new Boolean(true), "org.openl.j");
		_runNoError("String a=\"a\"; String b = \"b\"; (Object)(a + b) == (Object)(a + 'b')", new Boolean(false), "org.openl.j");


		
		_runNoError("int x=5, y=7; x & y", 5 & 7, "org.openl.j");
		_runNoError("int x=5, y=7; x | y", 5 | 7, "org.openl.j");
		_runNoError("int x=5, y=7; x ^ y", 5 ^ 7, "org.openl.j");

		_runNoError("boolean x=true, y=false; x ^ y", true ^ false, "org.openl.j");
		
		_runNoError("int x=5, y=7; x < y ? 'a'+1 : 'b'+1", 'a'+1, "org.openl.j");

		_runNoError("int x=5, y=7; x << y ", 5 << 7, "org.openl.j");
		_runNoError("long x=5;int y=7; x << y ", (long)5 << 7, "org.openl.j");

		_runNoError("long x=-1;int y=7; x >> y ", (long)-1 >> 7, "org.openl.j");
		_runNoError("long x=-1;int y=60; x >>> y ", (long)-1 >>> 60, "org.openl.j");
		
		
		_runNoError("System.out << 35 << \"zzzz\" ", System.out, "org.openl.j");
		
		_runNoError("|-10| ", 10, "org.openl.j");
		
		
		
		
		
	}

	
	public static void main(String[] args)
	{
		new RunTest().testRun();
	}
}
