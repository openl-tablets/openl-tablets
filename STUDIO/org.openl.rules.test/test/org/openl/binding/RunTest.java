package org.openl.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Test;
import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.DoubleValue;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.util.RangeWithBounds;
import org.openl.util.RangeWithBounds.BoundType;

public class RunTest {
    private static Object runExpression(String expression, SourceType parseType) {
        OpenL openl = OpenL.getInstance(OpenL.OPENL_JAVA_NAME);
        return OpenLManager.run(openl, new StringSourceCodeModule(expression, null), parseType);
    }

    private static void assertEqual(String expression, Object expected, SourceType parseType) {
        Object res = runExpression(expression, parseType);
        Assert.assertThat(res, new BaseMatcher<Object>() {
            @Override
            public boolean matches(Object item) {
                return Objects.deepEquals(res, expected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(expected);
            }
        });
    }

    private static <T> void assertEqual(String expression, T expected) {
        assertEqual(expression, expected, SourceType.METHOD_BODY);
    }

    private static void assertError(String expression, Class<? extends Throwable> expected, SourceType parseType) {
        Throwable ex = null;
        try {
            runExpression(expression, parseType);
        } catch (Throwable t) {
            ex = t;
        }
        Assert.assertNotNull(ex);
        Assert.assertEquals(expected, ex.getClass());
    }

    private static void assertError(String expr, Class<? extends Throwable> expected) {
        assertError(expr, expected, SourceType.METHOD_BODY);
    }

    @Test
    public void testBig() {
        assertEqual("Vector x = new Vector(); x.size()", 0);

        assertEqual("BigDecimal x = 10, y = 20; x > y", false);
        assertEqual("BigDecimal x = 10, y = 20; x < y", true);
        assertEqual("BigDecimal x = 10, y = 20; x + y == 30", true);
        assertEqual("BigDecimal x = 10, y = 20; x + y  - 5", new BigDecimal(25));

        assertEqual("BigInteger x = 10; BigDecimal y = x; x + x  - 5", new BigInteger("15", 10));
        assertEqual("BigInteger x = 10; BigDecimal y = x; x == y", true);
        assertEqual("BigInteger x = 10; BigDecimal y = x; y == x", true);

        assertEqual("BigInteger x = 10; x != null", true);
        assertEqual("BigInteger x = 10; null != x", true);

        assertEqual("BigDecimal x = 10; x != null", true);
        assertEqual("BigDecimal x = 10; null != x", true);

        assertEqual("BigInteger x = 10; x == null", false);
        assertEqual("BigInteger x = 10; null == x", false);

        assertEqual("BigDecimal x = 10; x == null", false);
        assertEqual("BigDecimal x = 10; null == x", false);

    }

    @Test
    public void testComparable() {
        assertEqual("String x = \"abc\";String y = \"abc\"; x < y", false);
        assertEqual("String x = \"abc\";String y = \"abc\"; x <= y", true);

        assertError("String x = \"abc\";Integer y = 10; x <= y", CompositeSyntaxNodeException.class);
    }

    @Test
    public void testDoubleRange() {
        assertEqual("10.0", new RangeWithBounds(10.0, 10.0), SourceType.DOUBLE_RANGE);
        assertEqual("< 10.0K",
            new RangeWithBounds(Double.NEGATIVE_INFINITY, 10000.0, BoundType.INCLUDING, BoundType.EXCLUDING),
            SourceType.DOUBLE_RANGE);
        assertEqual("<=33.5M", new RangeWithBounds(Double.NEGATIVE_INFINITY, 33500000.0), SourceType.DOUBLE_RANGE);
        assertEqual("5.0-$10.0", new RangeWithBounds(5.0, 10.0), SourceType.DOUBLE_RANGE);
        assertEqual(">2B",
            new RangeWithBounds(2000000000.0, Double.POSITIVE_INFINITY, BoundType.EXCLUDING, BoundType.INCLUDING),
            SourceType.DOUBLE_RANGE);
        assertEqual("2.1B+", new RangeWithBounds(2100000000.0, Double.POSITIVE_INFINITY), SourceType.DOUBLE_RANGE);

        assertError("10.0-2.0", CompositeSyntaxNodeException.class, SourceType.DOUBLE_RANGE);
        assertEqual("10.0-12,599.0", new RangeWithBounds(10.0, 12599.0), SourceType.DOUBLE_RANGE);
        assertEqual("$10,222.0 .. 12,599.0   ", new RangeWithBounds(10222.0, 12599.0), SourceType.DOUBLE_RANGE);

    }

    @Test
    public void testLong() {
        assertEqual("long x = 4; x + 5.0", 9.0);
        assertEqual("long x = 4; x - 5.0", -1.0);
        assertEqual("long x = 4; x - 5", -1L);
        assertEqual("Long x = null; x - 5.0", -5.0);

    }

    @Test
    public void testRange() {
        assertEqual("-1-2", new RangeWithBounds(-1, 2), SourceType.INT_RANGE);
        assertEqual("10", new RangeWithBounds(10, 10), SourceType.INT_RANGE);
        assertEqual("< 10K",
            new RangeWithBounds(Integer.MIN_VALUE, 10000, BoundType.INCLUDING, BoundType.EXCLUDING),
            SourceType.INT_RANGE);
        assertEqual("<=33.3M", new RangeWithBounds(Integer.MIN_VALUE, 33300000), SourceType.INT_RANGE);
        assertEqual("5-$10", new RangeWithBounds(5, 10), SourceType.INT_RANGE);
        assertEqual(">2B",
            new RangeWithBounds(2000000000, Integer.MAX_VALUE, BoundType.EXCLUDING, BoundType.INCLUDING),
            SourceType.INT_RANGE);
        assertEqual("2.1B+", new RangeWithBounds(2100000000, Integer.MAX_VALUE), SourceType.INT_RANGE);

        assertError("10-2", CompositeSyntaxNodeException.class, SourceType.INT_RANGE);
        assertEqual("10-12,599", new RangeWithBounds(10, 12599), SourceType.INT_RANGE);
        assertEqual("$10,222 .. 12,599   ", new RangeWithBounds(10222, 12599), SourceType.INT_RANGE);

    }

    @Test
    public void testRun() {

        assertEqual("String $x$y=null; $x$y == null || $x$y.length() < 10", true);

        assertEqual("String x=null; x == null || x.length() < 10", true);
        assertEqual("String x=null; x != null && x.length() < 10", false);

        assertEqual("String x=null; Boolean b = true; b || x.length() < 10", true);
        assertEqual("String x=null; Boolean b = false; b && x.length() < 10", false);

        assertEqual("String x=\"abc\"; x == null || x.length() < 10", true);
        assertEqual("String x=\"abc\"; x != null && x.length() < 10", true);

        assertEqual("int x = 5; x += 4", new Integer(9));
        assertEqual("DoubleValue d1 = new DoubleValue(5); DoubleValue d2 = new DoubleValue(4); d1 += d2; d1.getValue()",
            new Double(9));
        assertEqual("int i=0; for(int j=0; j < 10; ) {i += j;j++;} i", new Integer(45));

        // Testing new implementation of s1 == s2 for Strings. To achieve old
        // identity test Strings must be upcasted to Object
        assertEqual("String a=\"a\"; String b = \"b\"; a + b == a + 'b'", new Boolean(true));
        assertEqual("String a=\"a\"; String b = \"b\"; a + b == a + 'c'", new Boolean(false));
        assertEqual("String a=\"a\"; String b = \"b\"; a + b != a + 'b'", new Boolean(false));
        assertEqual("String a=\"a\"; String b = \"b\"; a + b != a + 'c'", new Boolean(true));
        assertEqual("String a=\"a\"; String b = \"b\"; (Object)(a + b) == (Object)(a + 'b')", new Boolean(true));
        assertEqual("String a=\"a\"; String b = \"b\"; (Object)(a + b) ==== (Object)(a + 'b')", new Boolean(false));

        assertEqual("boolean a=true; boolean b = false; a == !b", new Boolean(true));
        assertEqual("boolean a=true; boolean b = false; a != b", new Boolean(true));

        assertEqual("Integer x = 1; \"aaa\".substring(x)", "aaa".substring(1));

        assertEqual("int x=5, y=7; x & y", 5 & 7);
        assertEqual("int x=5, y=7; x | y", 5 | 7);
        assertEqual("int x=5, y=7; x ^ y", 5 ^ 7);

        assertEqual("boolean x=true, y=false; x ^ y", true ^ false);

        assertEqual("int x=5, y=7; x < y ? 'a'+1 : 'b'+1", 'a' + 1);
        assertEqual("int x=5, y=7; x < y ? 0.7 : 3", 0.7);
        assertEqual("int x=5, y=7; x >= y ? 3 : 0.7", 0.7);
        assertEqual("int x=5, y=7; x >= y ? null : 0.7", 0.7);
        assertEqual("int x=5, y=7; x < y ? 0.7 : null", 0.7);
        assertEqual("int x=5, y=7; x < y ? 3 : (int)0.7", 3);
        assertEqual("Number x=new Integer(5);Integer y = 7; 5 < 4 ? x : y", 7);

        assertEqual("int x=5, y=7; x << y ", 5 << 7);
        assertEqual("long x=5;int y=7; x << y ", (long) 5 << 7);

        assertEqual("long x=-1;int y=7; x >> y ", (long) -1 >> 7);
        assertEqual("long x=-1;int y=60; x >>> y ", (long) -1 >>> 60);

        assertEqual("System.out << 35 << \"zzzz\" ", System.out);

        assertEqual("|-10| ", 10);
        assertEqual("true ? 10 : 20", 10);
        assertEqual("true ? 10 : 20", 10);
        assertEqual("false ? 10 : 20", 20);

        assertEqual("10%", 0.1);
        assertEqual("10% of \n the  50", 5.0);
        assertEqual("10% of    the  50", 5.0);

        assertEqual("long Of =-1;int y=60; Of >>> y ", (long) -1 >>> 60);
        assertEqual("5.0 ** 7.0 ", Math.pow(5, 7));
        assertEqual("DoubleValue x = 5.0; x ** 7 ", new DoubleValue(Math.pow(5, 7)));
        assertEqual("BigDecimal x = 5.0; x ** 7 ", new BigDecimal("78125.0000000"));

        assertEqual("1 == 1", true);
        assertEqual("1 is same as 1", true);
        assertEqual("1 is same \n as 1", true);
        assertEqual("1   is   same   as   1", true);
        assertEqual("1 is the same as 1", true);
        assertEqual("1 is the \n same as 1", true);
        assertEqual("1   is   the   same   as   1", true);
        assertEqual("1 equals to 1", true);
        assertEqual("1 equals \n to 1", true);
        assertEqual("1  equals  to  1", true);

        assertEqual("not false", true);

        assertEqual("true and true", true);
        assertEqual("(true)and(true)", true);
        assertEqual("true and false", false);
        assertEqual("(true)and(false)", false);
        assertEqual("true or false", true);
        assertEqual("(true)or(false)", true);
        assertEqual("(false)or(false)", false);
        assertEqual("false or false", false);

        assertEqual("1 does not equal to 2", true);
        assertEqual("2 does not \n     equal to 2", false);

        assertEqual("1 is different from 2", true);
        assertEqual("2 is different \n  from 2", false);

        assertEqual("1 is less than 2", true);
        assertEqual("2 is \n less    than 2", false);

        assertEqual("2 is more than 1", true);
        assertEqual("2 is \n more    than 2", false);

        assertEqual("1 is less or equal 1", true);
        assertEqual("2 is \n less or   equal 1", false);

        assertEqual("1 is no more than 1", true);
        assertEqual("2 is \n no more   than 1", false);

        assertEqual("1 is in 1", true);
        assertEqual("1 is \n     in 1", true);

        assertEqual("1 is more or equal 1", true);
        assertEqual("1 is \n more or   equal 2", false);

        assertEqual("1 is no less than 1", true);
        assertEqual("1 is \n no less     than 2", false);

    }

    @Test
    public void testAggregate() {

        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[2]", "aaa");
        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[!@ startsWith(\"b\")]", "bb");

        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int x = 3; ary[@ length() == x][0]", "ddd");

        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[^@ substring(1,2)][0]", "aaa");
        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[^@ substring(1)][1]", "bb");
        assertEqual("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[^@ substring(0,1)][0]", "aab");
        assertEqual("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[v@ substring(0,1)][0]", "ddd");

        assertEqual("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)].length", 2);
        assertEqual("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0].length", 3);
        assertEqual("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][1].length", 1);
        assertEqual("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0][2]", "aaba");

        assertEqual("String[] ary = {\"daab\",\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0][1]",
            "ddd");

        assertEqual("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[*@ substring(0,1)].length", 4);
        assertEqual("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[*!@ substring(0,1)].length", 2);

        // test named element

        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int x = 3; ary[(String s) @ length() == x][0]", "ddd");
        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) !@ s.startsWith(\"b\")]", "bb");
        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[( s ) !@ s.startsWith(\"aa\")]", "aaa");
        // Index variable in parentheses must not be parsed as aggregate operator
        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int i = 1; ary[ (i) ]", "ddd");

        assertEqual(
            "String[] ary = {\"bbcc\", \"dddee\",\"aaabbe\" ,\"aaabb\",}; ary[!@startsWith (ary[( s ) !@ s.toUpperCase().endsWith(\"BB\")])]",
            "aaabbe");

        assertEqual(
            "String[] ary = {\"bbccdd\", \"dddee\",\"dddeedd\", \"ddd\" ,\"aaabbdd\",}; ary[(s1)!@ s1.startsWith (ary[( s2 ) !@ s2.toUpperCase().startsWith(\"DD\") && s2 != s1])]",
            "dddeedd");

        assertEqual(
            "String[] ary1 = {\"bbccdd\", \"dddee\",\"dddeedd\", \"ddd\" ,\"aaabbdd\",}; String[] ary2 = {\"ZZZ\", \"XXXX\",\"YYYYYY\", \"ddd\" ,\"aaabbdd\",}; ary1[(s)!@ s.startsWith(\"aa\")] + ary2[(s)!@ s.startsWith(\"ZZ\")]",
            "aaabbddZZZ");

        assertError("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) !@ s.getDay() < 5]",
            CompositeSyntaxNodeException.class);
        assertError("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(Date d) !@ d.getDay() < 5]",
            CompositeSyntaxNodeException.class);

        assertEqual("String[] ary = {\"a\", \"b\",\"c\" ,\"a\",\"d\",\"b\",}; ary[(x)~@ x][(str)*@str[0]].length", 4);

        // test lists

        assertEqual(
            "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list.get(0)",
            "bbccdd");

        assertError(
            "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(Date d)!@ d.getDay() < 6]",
            OpenLRuntimeException.class);
        assertEqual(
            "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(String s)!@ s.contains(\"ee\")]",
            "dddee");
        assertEqual(
            "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(String str) select first where str.contains(\"ee\")]",
            "dddee");

        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); int x = 3; list[(String s) @ length() == x][0]",
            "ddd");
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); int x = 3; list[(String s) select all having length() == x][0]",
            "ddd");

        assertEqual(
            "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) ^@ toLowerCase()][0]",
            "aab");
        assertEqual(
            "List list = Arrays.asList(\"aab\", \"ddd\", \"aac\", \"aaba\"); list[(String s) v@  substring(0,1)][0]",
            "ddd");

        assertEqual(
            "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) ~@ substring(0,1)][1][0]",
            "ddd");

        assertEqual(
            "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) *@ substring(0,1)].length",
            4);
        assertEqual(
            "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) *!@ substring(0,1).toLowerCase()].length",
            2);

        // Test spaces
        assertEqual("String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order by toString() ][0]", "aaba");
        assertEqual("String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order \n      by toString() ][0]", "aaba");
        assertEqual(
            "String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order \n      increasing by toString() ][0]",
            "aaba");
        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[ !@ startsWith(\"d\") ]", "ddd");
    }

    @Test
    public void testSelectAllForList() {
        // Select all should return array
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all having length() == 3]",
            new String[] { "ddd", "aaa" });
        // Demonstrate that no need to cast to String if "select all" returns array instead of List
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add(\"aaaa\"); list[(String a) select all having a.startsWith(\"a\")][0].length()",
            4);
        // Select all with spaces between words
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all \n    having length() == 3]",
            new String[] { "ddd", "aaa" });
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all \n     where length() == 3]",
            new String[] { "ddd", "aaa" });
    }

    @Test
    public void testOrderByForList() {
        // Order by should return array
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order by s]",
            new String[] { "aaa", "bb", "ddd" });
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order decreasing by s]",
            new String[] { "ddd", "bb", "aaa" });
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order \n     decreasing by s]",
            new String[] { "ddd", "bb", "aaa" });
    }

    @Test
    public void testTransformForList() {
        // Transform to should return array
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform to s + s.length()]",
            new String[] { "bb2", "ddd3", "aaa3" });
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform \n     to s + s.length()]",
            new String[] { "bb2", "ddd3", "aaa3" });
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform \n     unique to s + s.length()]",
            new String[] { "bb2", "ddd3", "aaa3" });
        // Transform String array to array of List
        assertEqual("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) transform to Arrays.asList(s)]",
            new List[] { Collections.singletonList("bb"),
                    Collections.singletonList("ddd"),
                    Collections.singletonList("aaa") });
        // Transform List to List of Lists
        assertEqual(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform to Arrays.asList(s)]",
            new List[] { Collections.singletonList("bb"),
                    Collections.singletonList("ddd"),
                    Collections.singletonList("aaa") });
    }

    @Test
    public void testSplitByForList() {
        // Split By should return array of arrays
        assertEqual(
            "List list = new ArrayList(); list.add(\"5000\");list.add( \"2002\");list.add(\"3300\");list.add(\"2113\"); list[(String s) split by substring(0,1)]",
            new String[][] { { "5000" }, { "2002", "2113" }, { "3300" } });

        // Split By when list contains Lists
        assertEqual(
            "List list = new ArrayList(); list.add(Arrays.asList(1));list.add(Arrays.asList(2, 3));list.add(Arrays.asList(4)); list[(List l) split by size()]",
            new List[][] { { Collections.singletonList(1), Collections.singletonList(4) }, { Arrays.asList(2, 3) } });
        // Check array element type. It must be List, not Object (Object doesn't contain method size()).
        assertEqual(
            "List list = new ArrayList(); list.add(Arrays.asList(1));list.add(Arrays.asList(2, 3));list.add(Arrays.asList(4)); list[(List l) split by size()][0][1].size()",
            1);
    }

    @Test
    public void testArrayOfList() {
        // When array has the type List[] then inside of aggregate function array element type must be List, not Object.
        assertEqual("List[] ary = {Arrays.asList(1), Arrays.asList(2, 3), Arrays.asList(4)}; ary[split by size()]",
            new List[][] { { Collections.singletonList(1), Collections.singletonList(4) }, { Arrays.asList(2, 3) } });
        assertEqual(
            "List[] ary = {Arrays.asList(1), Arrays.asList(2, 3), Arrays.asList(4)}; ary[split \n    by size()]",
            new List[][] { { Collections.singletonList(1), Collections.singletonList(4) }, { Arrays.asList(2, 3) } });
    }

    @Test
    public void testStatic() {
        assertEqual("int.class", int.class);
        assertEqual("Double.class", Double.class);
        assertEqual("String.valueOf(5)", String.valueOf(5));
        assertEqual("Math.PI", Math.PI);
        assertEqual("\"c\".getClass()", "c".getClass());
        assertEqual("\"c\".class", String.class); // getClass()

        assertEqual("'c'.class", char.class);
        assertEqual("1.1.class", double.class);
        assertEqual("10d.class", double.class);
        assertEqual("int x = 5; x.class", int.class);

        assertError("String.length()", CompositeSyntaxNodeException.class);
        assertError("Double.isNaN()", CompositeSyntaxNodeException.class);
        assertError("Double.getClass()", CompositeSyntaxNodeException.class);
        assertError("int.getClass()", CompositeSyntaxNodeException.class);
    }

    @Test
    public void testLongName() {
        assertEqual("new java.math.BigDecimal(10)", new java.math.BigDecimal(10));
        assertEqual("java.io.File f = new java.io.File(\"./temp\"); f.getName()", "temp");
    }

    @Test
    public void testArrayInitializationObject() {
        assertEqual("new Byte[0]", new Byte[0]);
        assertEqual("new Byte[5]", new Byte[5]);
        assertEqual("new Byte[]{1,2,3}", new Byte[] { 1, 2, 3 });
        assertEqual("new Byte[3][]", new Byte[3][]);
        assertEqual("new Byte[3][5]", new Byte[3][5]);
        assertEqual("new Byte[3][5][]", new Byte[3][5][]);
        assertEqual("new Byte[]{}", new Byte[] {});
        assertEqual("new Byte[][]{}", new Byte[][] {});
        assertEqual("new Byte[][]{{},{4}}", new Byte[][] { {}, { 4 } });
        assertEqual("new Byte[][]{{1,2,3},{4}}", new Byte[][] { { 1, 2, 3 }, { 4 } });
        assertEqual("new Byte[]{4}", new Byte[] { 4 });
    }

    @Test
    public void testArrayInitializationPrimitives() {
        assertEqual("new int[0]", new int[0]);
        assertEqual("new int[5]", new int[5]);
        assertEqual("new int[]{1,2,3}", new int[] { 1, 2, 3 });
        assertEqual("new int[3][]", new int[3][]);
        assertEqual("new int[3][5]", new int[3][5]);
        assertEqual("new int[3][5][]", new int[3][5][]);
        assertEqual("new int[]{}", new int[] {});
        assertEqual("new int[][]{}", new int[][] {});
        assertEqual("new int[][]{{},{4}}", new int[][] { {}, { 4 } });
        assertEqual("new int[][]{{1,2,3},{4}}", new int[][] { { 1, 2, 3 }, { 4 } });
        assertEqual("new int[]{4}", new int[] { 4 });
    }
}
