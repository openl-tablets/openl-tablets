package org.openl.binding;

import java.math.BigDecimal;
import java.math.BigInteger;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.meta.DoubleValue;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.util.RangeWithBounds;
import org.openl.util.RangeWithBounds.BoundType;

public class RunTest extends TestCase {
    private static interface AssertionExpression<T> {
        void makeAssertion(T expected, T result);
    }

    private static AssertionExpression<Object> equalsAssertion = new AssertionExpression<Object>() {
        public void makeAssertion(Object expected, Object result) {
            Assert.assertEquals(expected, result);
        }
    };

    private static AssertionExpression<Object> nopAssertion = new AssertionExpression<Object>() {
        public void makeAssertion(Object expected, Object result) {
            // do nothing
        }
    };

    public static void main(String[] args) {
        new RunTest().testRun();
    }

    void _runNoError(String expr, Object expected, String openl) {
        _runNoError(expr, expected, openl, SourceType.METHOD_BODY);
    }

    void _runNoError(String expr, Object expected, String openlName, SourceType parseType) {
        _runNoError(expr, expected, openlName, parseType, equalsAssertion);
    }

    @SuppressWarnings("unchecked")
    static void _runNoError(String expression, Object expected, String openlName, SourceType parseType,
            AssertionExpression assertion) {
        OpenL openl = OpenL.getInstance(openlName);
        Object res = OpenLManager.run(openl, new StringSourceCodeModule(expression, null), parseType);
        assertion.makeAssertion(expected, res);
    }

    void _runWithError(String expr, Object expected, String openl, SourceType parseType) {
        Throwable ex = null;
        try {
            _runNoError(expr, expected, openl, parseType, nopAssertion);
        } catch (Throwable t) {
            ex = t;
        }
        Assert.assertNotNull(ex);
        Assert.assertEquals(expected, ex.getClass());

    }

    public void testBig() {
        _runNoError("Vector x = new Vector(); x.size()", 0, "org.openl.j");

        _runNoError("BigDecimal x = 10, y = 20; x > y", false, "org.openl.j");
        _runNoError("BigDecimal x = 10, y = 20; x < y", true, "org.openl.j");
        _runNoError("BigDecimal x = 10, y = 20; x + y == 30", true, "org.openl.j");
        _runNoError("BigDecimal x = 10, y = 20; x + y  - 5", new BigDecimal(25), "org.openl.j");

        _runNoError("BigInteger x = 10; BigDecimal y = x; x + x  - 5", new BigInteger("15", 10), "org.openl.j");
        _runNoError("BigInteger x = 10; BigDecimal y = x; x == y", true, "org.openl.j");
        _runNoError("BigInteger x = 10; BigDecimal y = x; y == x", true, "org.openl.j");

        _runNoError("BigInteger x = 10; x != null", true, "org.openl.j");
        _runNoError("BigInteger x = 10; null != x", true, "org.openl.j");

        _runNoError("BigDecimal x = 10; x != null", true, "org.openl.j");
        _runNoError("BigDecimal x = 10; null != x", true, "org.openl.j");

        _runNoError("BigInteger x = 10; x == null", false, "org.openl.j");
        _runNoError("BigInteger x = 10; null == x", false, "org.openl.j");

        _runNoError("BigDecimal x = 10; x == null", false, "org.openl.j");
        _runNoError("BigDecimal x = 10; null == x", false, "org.openl.j");

    }

    public void testComparable() {
        _runNoError("String x = \"abc\";String y = \"abc\"; x < y", false, "org.openl.j");
        _runNoError("String x = \"abc\";String y = \"abc\"; x <= y", true, "org.openl.j");

        // TODO fix String < Integer - must be compile time error
        // _runNoError("String x = \"abc\";Integer y = 10; x <= y", true,
        // "org.openl.j");
    }

    public void testDoubleRange() {
        AssertionExpression<RangeWithBounds> assertion = new AssertionExpression<RangeWithBounds>() {
            public void makeAssertion(RangeWithBounds expected, RangeWithBounds result) {
                Assert.assertEquals(expected.getMax().doubleValue(), result.getMax().doubleValue(), 0.001);
                Assert.assertEquals(expected.getMin().doubleValue(), result.getMin().doubleValue(), 0.001);
            }
        };

        _runNoError("10.0", new RangeWithBounds(10.0, 10.0), "org.openl.j", SourceType.DOUBLE_RANGE, assertion);
        _runNoError("< 10.0K", new RangeWithBounds(Double.NEGATIVE_INFINITY, 9999.99999999999), "org.openl.j",
                SourceType.DOUBLE_RANGE, assertion);
        _runNoError("<=33.3M", new RangeWithBounds(Double.NEGATIVE_INFINITY, 33300000.0), "org.openl.j",
                SourceType.DOUBLE_RANGE, assertion);
        _runNoError("5.0-$10.0", new RangeWithBounds(5.0, 10.0), "org.openl.j", SourceType.DOUBLE_RANGE, assertion);
        _runNoError("2B<", new RangeWithBounds(2000000000.0001, Double.POSITIVE_INFINITY), "org.openl.j",
                SourceType.DOUBLE_RANGE, assertion);
        _runNoError("2.1B+", new RangeWithBounds(2100000000, Double.POSITIVE_INFINITY), "org.openl.j",
                SourceType.DOUBLE_RANGE, assertion);

        _runWithError("10.0-2.0", CompositeSyntaxNodeException.class, "org.openl.j", SourceType.DOUBLE_RANGE);
        _runNoError("10.0-12,599.0", new RangeWithBounds(10.0, 12599.0), "org.openl.j", SourceType.DOUBLE_RANGE, assertion);
        _runNoError("$10,222.0 .. 12,599.0   ", new RangeWithBounds(10222.0, 12599.0), "org.openl.j",
                SourceType.DOUBLE_RANGE, assertion);

    }

    public void testLong() {
        _runNoError("long x = 4; x + 5.0", 9.0, "org.openl.j");
        _runNoError("long x = 4; x - 5.0", -1.0, "org.openl.j");
        _runNoError("long x = 4; x - 5", -1L, "org.openl.j");
//        _runNoError("Long x = null; x - 5.0", -1.0, "org.openl.j");
        
    }

    public void testRange() {
        // _runNoError("-1-2", new RangeWithBounds(10, 10), "org.openl.j",
        // "range.literal");
        _runNoError("10", new RangeWithBounds(10, 10), "org.openl.j", SourceType.INT_RANGE);
        _runNoError("< 10K", new RangeWithBounds(Integer.MIN_VALUE, 10000, BoundType.INCLUDING, BoundType.EXCLUDING), "org.openl.j", SourceType.INT_RANGE);
        _runNoError("<=33.3M", new RangeWithBounds(Integer.MIN_VALUE, 33300000), "org.openl.j", SourceType.INT_RANGE);
        _runNoError("5-$10", new RangeWithBounds(5, 10), "org.openl.j", SourceType.INT_RANGE);
        _runNoError("2B<", new RangeWithBounds(2000000000, Integer.MAX_VALUE, BoundType.EXCLUDING, BoundType.INCLUDING), "org.openl.j", SourceType.INT_RANGE);
        _runNoError("2.1B+", new RangeWithBounds(2100000000, Integer.MAX_VALUE), "org.openl.j", SourceType.INT_RANGE);

        _runWithError("10-2", CompositeSyntaxNodeException.class, "org.openl.j", SourceType.INT_RANGE);
        _runNoError("10-12,599", new RangeWithBounds(10, 12599), "org.openl.j", SourceType.INT_RANGE);
        _runNoError("$10,222 .. 12,599   ", new RangeWithBounds(10222, 12599), "org.openl.j", SourceType.INT_RANGE);

    }

    public void testRun() {

        _runNoError("String $x$y=null; $x$y == null || $x$y.length() < 10", true, "org.openl.j");

        _runNoError("String x=null; x == null || x.length() < 10", true, "org.openl.j");
        _runNoError("String x=null; x != null && x.length() < 10", false, "org.openl.j");

        _runNoError("String x=null; Boolean b = true; b || x.length() < 10", true, "org.openl.j");
        _runNoError("String x=null; Boolean b = false; b && x.length() < 10", false, "org.openl.j");

        _runNoError("String x=\"abc\"; x == null || x.length() < 10", true, "org.openl.j");
        _runNoError("String x=\"abc\"; x != null && x.length() < 10", true, "org.openl.j");

        _runNoError("int x = 5; x += 4", new Integer(9), "org.openl.j");
        _runNoError(
                "DoubleValue d1 = new DoubleValue(5); DoubleValue d2 = new DoubleValue(4); d1 += d2; d1.getValue()",
                new Double(9), "org.openl.rules.java");
        _runNoError("int i=0; for(int j=0; j < 10; ) {i += j;j++;} i", new Integer(45), "org.openl.j");


        
        // Testing new implementation of s1 == s2 for Strings. To achieve old
        // identity test Strings must be downcasted to Object
        _runNoError("String a=\"a\"; String b = \"b\"; a + b == a + 'b'", new Boolean(true), "org.openl.j");
        _runNoError("String a=\"a\"; String b = \"b\"; a + b == a + 'c'", new Boolean(false), "org.openl.j");
        _runNoError("String a=\"a\"; String b = \"b\"; a + b != a + 'b'", new Boolean(false), "org.openl.j");
        _runNoError("String a=\"a\"; String b = \"b\"; a + b != a + 'c'", new Boolean(true), "org.openl.j");
        _runNoError("String a=\"a\"; String b = \"b\"; (Object)(a + b) == (Object)(a + 'b')", new Boolean(false),
                "org.openl.j");

        
        _runNoError("boolean a=true; boolean b = false; a == !b", new Boolean(true), "org.openl.j");
        _runNoError("boolean a=true; boolean b = false; a != b", new Boolean(true), "org.openl.j");
        
        _runNoError("Integer x = 1; \"aaa\".substring(x)", "aaa".substring(1), "org.openl.j");

        
        
        _runNoError("int x=5, y=7; x & y", 5 & 7, "org.openl.j");
        _runNoError("int x=5, y=7; x | y", 5 | 7, "org.openl.j");
        _runNoError("int x=5, y=7; x ^ y", 5 ^ 7, "org.openl.j");

        _runNoError("boolean x=true, y=false; x ^ y", true ^ false, "org.openl.j");

        _runNoError("int x=5, y=7; x < y ? 'a'+1 : 'b'+1", 'a' + 1, "org.openl.j");
        _runNoError("int x=5, y=7; x < y ? 0.7 : 3", 0.7, "org.openl.j");
        _runNoError("int x=5, y=7; x < y ? 3 : (int)0.7", 3, "org.openl.j");

        _runNoError("int x=5, y=7; x << y ", 5 << 7, "org.openl.j");
        _runNoError("long x=5;int y=7; x << y ", (long) 5 << 7, "org.openl.j");

        _runNoError("long x=-1;int y=7; x >> y ", (long) -1 >> 7, "org.openl.j");
        _runNoError("long x=-1;int y=60; x >>> y ", (long) -1 >>> 60, "org.openl.j");

        _runNoError("System.out << 35 << \"zzzz\" ", System.out, "org.openl.j");

        _runNoError("|-10| ", 10, "org.openl.j");
        _runNoError("true ? 10 : 20", 10, "org.openl.j");
        _runNoError("true ? 10 : 20", 10, "org.openl.j");
        _runNoError("false ? 10 : 20", 20, "org.openl.j");

        _runNoError("10%", 0.1, "org.openl.j");
        _runNoError("10% of \n the  50", 5.0, "org.openl.j");

        _runNoError("long Of =-1;int y=60; Of >>> y ", (long) -1 >>> 60, "org.openl.j");
        _runNoError("5.0 ** 7.0 ", Math.pow(5, 7), "org.openl.rules.java");
        _runNoError("DoubleValue x = 5.0; x ** 7 ", new DoubleValue(Math.pow(5, 7)), "org.openl.rules.java");
        _runNoError("BigDecimal x = 5.0; x ** 7 ", new BigDecimal(Math.pow(5, 7)), "org.openl.rules.java");

    }
    
    public void testStatic()
    {
        _runNoError("int.class", int.class, "org.openl.j");
        _runWithError("String.length()", CompositeSyntaxNodeException.class, "org.openl.j", SourceType.METHOD_BODY);
        

//        _runWithError("int x = 5; x.class", SyntaxNodeException.class, "org.openl.j", SourceType.METHOD_BODY);
    }
    
    
    public void testLongName()
    {
        _runNoError("new java.math.BigDecimal(10)", new java.math.BigDecimal(10), "org.openl.j");
        _runNoError("java.io.File f = new java.io.File(\"c:\temp\"); f.getParent()", "c:", "org.openl.j");
    }
    
}
