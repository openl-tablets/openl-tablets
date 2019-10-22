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

public class RunTest {
    private static Object runExpression(String expression, SourceType parseType) {
        OpenL openl = OpenL.getInstance(OpenL.OPENL_JAVA_NAME);
        return OpenLManager.run(openl, new StringSourceCodeModule(expression, null), parseType);
    }

    private static void assertEquals(String expression, Object expected, SourceType parseType) {
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

    private static <T> void assertEquals(String expression, T expected) {
        assertEquals(expression, expected, SourceType.METHOD_BODY);
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
        assertEquals("Vector x = new Vector(); x.size()", 0);

        assertEquals("BigDecimal x = 10, y = 20; x > y", false);
        assertEquals("BigDecimal x = 10, y = 20; x < y", true);
        assertEquals("BigDecimal x = 10, y = 20; x + y == 30", true);
        assertEquals("BigDecimal x = 10, y = 20; x + y  - 5", new BigDecimal(25));

        assertEquals("BigInteger x = 10; BigDecimal y = x; x + x  - 5", new BigInteger("15", 10));
        assertEquals("BigInteger x = 10; BigDecimal y = x; x == y", true);
        assertEquals("BigInteger x = 10; BigDecimal y = x; y == x", true);

        assertEquals("BigInteger x = 10; x != null", true);
        assertEquals("BigInteger x = 10; null != x", true);

        assertEquals("BigDecimal x = 10; x != null", true);
        assertEquals("BigDecimal x = 10; null != x", true);

        assertEquals("BigInteger x = 10; x == null", false);
        assertEquals("BigInteger x = 10; null == x", false);

        assertEquals("BigDecimal x = 10; x == null", false);
        assertEquals("BigDecimal x = 10; null == x", false);

    }

    @Test
    public void testComparable() {
        assertEquals("String x = \"abc\";String y = \"abc\"; x < y", false);
        assertEquals("String x = \"abc\";String y = \"abc\"; x <= y", true);

        assertError("String x = \"abc\";Integer y = 10; x <= y", CompositeSyntaxNodeException.class);
    }

    @Test
    public void testLong() {
        assertEquals("long x = 4; x + 5.0", 9.0);
        assertEquals("long x = 4; x - 5.0", -1.0);
        assertEquals("long x = 4; x - 5", -1L);
        assertEquals("Long x = null; x - 5.0", -5.0);

    }

    @Test
    public void testRun() {

        assertEquals("String $x$y=null; $x$y == null || $x$y.length() < 10", true);

        assertEquals("String x=null; x == null || x.length() < 10", true);
        assertEquals("String x=null; x != null && x.length() < 10", false);

        assertEquals("String x=null; Boolean b = true; b || x.length() < 10", true);
        assertEquals("String x=null; Boolean b = false; b && x.length() < 10", false);

        assertEquals("String x=\"abc\"; x == null || x.length() < 10", true);
        assertEquals("String x=\"abc\"; x != null && x.length() < 10", true);

        assertEquals("int x = 5; x += 4", new Integer(9));
        assertEquals(
            "DoubleValue d1 = new DoubleValue(5); DoubleValue d2 = new DoubleValue(4); d1 += d2; d1.getValue()",
            new Double(9));
        assertEquals("int i=0; for(int j=0; j < 10; ) {i += j;j++;} i", new Integer(45));

        // Testing new implementation of s1 == s2 for Strings. To achieve old
        // identity test Strings must be upcasted to Object
        assertEquals("String a=\"a\"; String b = \"b\"; a + b == a + 'b'", new Boolean(true));
        assertEquals("String a=\"a\"; String b = \"b\"; a + b == a + 'c'", new Boolean(false));
        assertEquals("String a=\"a\"; String b = \"b\"; a + b != a + 'b'", new Boolean(false));
        assertEquals("String a=\"a\"; String b = \"b\"; a + b != a + 'c'", new Boolean(true));
        assertEquals("String a=\"a\"; String b = \"b\"; (Object)(a + b) == (Object)(a + 'b')", new Boolean(true));
        assertEquals("String a=\"a\"; String b = \"b\"; (Object)(a + b) ==== (Object)(a + 'b')", new Boolean(false));

        assertEquals("boolean a=true; boolean b = false; a == !b", new Boolean(true));
        assertEquals("boolean a=true; boolean b = false; a != b", new Boolean(true));

        assertEquals("Integer x = 1; \"aaa\".substring(x)", "aaa".substring(1));

        assertEquals("int x=5, y=7; x & y", 5 & 7);
        assertEquals("int x=5, y=7; x | y", 5 | 7);
        assertEquals("int x=5, y=7; x ^ y", 5 ^ 7);

        assertEquals("boolean x=true, y=false; x ^ y", true ^ false);

        assertEquals("int x=5, y=7; x < y ? 'a'+1 : 'b'+1", 'a' + 1);
        assertEquals("int x=5, y=7; x < y ? 0.7 : 3", 0.7);
        assertEquals("int x=5, y=7; x >= y ? 3 : 0.7", 0.7);
        assertEquals("int x=5, y=7; x >= y ? null : 0.7", 0.7);
        assertEquals("int x=5, y=7; x < y ? 0.7 : null", 0.7);
        assertEquals("int x=5, y=7; x < y ? 3 : (int)0.7", 3);
        assertEquals("Number x=new Integer(5);Integer y = 7; 5 < 4 ? x : y", 7);

        assertEquals("int x=5, y=7; x << y ", 5 << 7);
        assertEquals("long x=5;int y=7; x << y ", (long) 5 << 7);

        assertEquals("long x=-1;int y=7; x >> y ", (long) -1 >> 7);
        assertEquals("long x=-1;int y=60; x >>> y ", (long) -1 >>> 60);

        assertEquals("System.out << 35 << \"zzzz\" ", System.out);

        assertEquals("|-10| ", 10);
        assertEquals("true ? 10 : 20", 10);
        assertEquals("true ? 10 : 20", 10);
        assertEquals("false ? 10 : 20", 20);

        assertEquals("10%", 0.1);
        assertEquals("10% of \n the  50", 5.0);
        assertEquals("10% of    the  50", 5.0);

        assertEquals("long Of =-1;int y=60; Of >>> y ", (long) -1 >>> 60);
        assertEquals("5.0 ** 7.0 ", Math.pow(5, 7));
        assertEquals("DoubleValue x = 5.0; x ** 7 ", new DoubleValue(Math.pow(5, 7)));
        assertEquals("BigDecimal x = 5.0; x ** 7 ", new BigDecimal("78125.0000000"));

        assertEquals("1 == 1", true);
        assertEquals("1 is same as 1", true);
        assertEquals("1 is same \n as 1", true);
        assertEquals("1   is   same   as   1", true);
        assertEquals("1 is the same as 1", true);
        assertEquals("1 is the \n same as 1", true);
        assertEquals("1   is   the   same   as   1", true);
        assertEquals("1 equals to 1", true);
        assertEquals("1 equals \n to 1", true);
        assertEquals("1  equals  to  1", true);

        assertEquals("not false", true);

        assertEquals("true and true", true);
        assertEquals("(true)and(true)", true);
        assertEquals("true and false", false);
        assertEquals("(true)and(false)", false);
        assertEquals("true or false", true);
        assertEquals("(true)or(false)", true);
        assertEquals("(false)or(false)", false);
        assertEquals("false or false", false);

        assertEquals("1 does not equal to 2", true);
        assertEquals("2 does not \n     equal to 2", false);

        assertEquals("1 is different from 2", true);
        assertEquals("2 is different \n  from 2", false);

        assertEquals("1 is less than 2", true);
        assertEquals("2 is \n less    than 2", false);

        assertEquals("2 is more than 1", true);
        assertEquals("2 is \n more    than 2", false);

        assertEquals("1 is less or equal 1", true);
        assertEquals("2 is \n less or   equal 1", false);

        assertEquals("1 is no more than 1", true);
        assertEquals("2 is \n no more   than 1", false);

        assertEquals("1 is in 1", true);
        assertEquals("1 is \n     in 1", true);

        assertEquals("1 is more or equal 1", true);
        assertEquals("1 is \n more or   equal 2", false);

        assertEquals("1 is no less than 1", true);
        assertEquals("1 is \n no less     than 2", false);

    }

    @Test
    public void testBusinessLiteral() {
        assertEquals(" 10M is more or equal 1K", true);
        assertEquals(" 10K is more or equal 1L", true);
        assertEquals(" 1K is more or equal 100", true);
        assertEquals(" -1K is more or equal 100", false);
    }

    @Test
    public void testAggregate() {

        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[2]", "aaa");
        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[!@ startsWith(\"b\")]", "bb");

        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int x = 3; ary[@ length() == x][0]", "ddd");

        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[^@ substring(1,2)][0]", "aaa");
        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[^@ substring(1)][1]", "bb");
        assertEquals("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[^@ substring(0,1)][0]", "aab");
        assertEquals("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[v@ substring(0,1)][0]", "ddd");

        assertEquals("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)].length", 2);
        assertEquals("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0].length", 3);
        assertEquals("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][1].length", 1);
        assertEquals("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0][2]", "aaba");

        assertEquals("String[] ary = {\"daab\",\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0][1]",
            "ddd");

        assertEquals("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[*@ substring(0,1)].length", 4);
        assertEquals("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[*!@ substring(0,1)].length", 2);

        // test named element

        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int x = 3; ary[(String s) @ length() == x][0]", "ddd");
        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) !@ s.startsWith(\"b\")]", "bb");
        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[( s ) !@ s.startsWith(\"aa\")]", "aaa");
        // Index variable in parentheses must not be parsed as aggregate operator
        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int i = 1; ary[ (i) ]", "ddd");

        assertEquals(
            "String[] ary = {\"bbcc\", \"dddee\",\"aaabbe\" ,\"aaabb\",}; ary[!@startsWith (ary[( s ) !@ s.toUpperCase().endsWith(\"BB\")])]",
            "aaabbe");

        assertEquals(
            "String[] ary = {\"bbccdd\", \"dddee\",\"dddeedd\", \"ddd\" ,\"aaabbdd\",}; ary[(s1)!@ s1.startsWith (ary[( s2 ) !@ s2.toUpperCase().startsWith(\"DD\") && s2 != s1])]",
            "dddeedd");

        assertEquals(
            "String[] ary1 = {\"bbccdd\", \"dddee\",\"dddeedd\", \"ddd\" ,\"aaabbdd\",}; String[] ary2 = {\"ZZZ\", \"XXXX\",\"YYYYYY\", \"ddd\" ,\"aaabbdd\",}; ary1[(s)!@ s.startsWith(\"aa\")] + ary2[(s)!@ s.startsWith(\"ZZ\")]",
            "aaabbddZZZ");

        assertError("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) !@ s.getDay() < 5]",
            CompositeSyntaxNodeException.class);
        assertError("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(Date d) !@ d.getDay() < 5]",
            CompositeSyntaxNodeException.class);

        assertEquals("String[] ary = {\"a\", \"b\",\"c\" ,\"a\",\"d\",\"b\",}; ary[(x)~@ x][(str)*@str[0]].length", 4);

        // test lists

        assertEquals(
            "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list.get(0)",
            "bbccdd");

        assertError(
            "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(Date d)!@ d.getDay() < 6]",
            OpenLRuntimeException.class);
        assertEquals(
            "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(String s)!@ s.contains(\"ee\")]",
            "dddee");
        assertEquals(
            "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(String str) select first where str.contains(\"ee\")]",
            "dddee");

        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); int x = 3; list[(String s) @ length() == x][0]",
            "ddd");
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); int x = 3; list[(String s) select all having length() == x][0]",
            "ddd");

        assertEquals(
            "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) ^@ toLowerCase()][0]",
            "aab");
        assertEquals(
            "List list = Arrays.asList(\"aab\", \"ddd\", \"aac\", \"aaba\"); list[(String s) v@  substring(0,1)][0]",
            "ddd");

        assertEquals(
            "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) ~@ substring(0,1)][1][0]",
            "ddd");

        assertEquals(
            "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) *@ substring(0,1)].length",
            4);
        assertEquals(
            "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) *!@ substring(0,1).toLowerCase()].length",
            2);

        // Test spaces
        assertEquals("String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order by toString() ][0]", "aaba");
        assertEquals("String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order \n      by toString() ][0]",
            "aaba");
        assertEquals(
            "String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order \n      increasing by toString() ][0]",
            "aaba");
        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[ !@ startsWith(\"d\") ]", "ddd");
    }

    @Test
    public void testSelectAllForList() {
        // Select all should return array
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all having length() == 3]",
            new String[] { "ddd", "aaa" });
        // Demonstrate that no need to cast to String if "select all" returns array instead of List
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add(\"aaaa\"); list[(String a) select all having a.startsWith(\"a\")][0].length()",
            4);
        // Select all with spaces between words
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all \n    having length() == 3]",
            new String[] { "ddd", "aaa" });
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all \n     where length() == 3]",
            new String[] { "ddd", "aaa" });
    }

    @Test
    public void testOrderByForList() {
        // Order by should return array
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order by s]",
            new String[] { "aaa", "bb", "ddd" });
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order decreasing by s]",
            new String[] { "ddd", "bb", "aaa" });
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order \n     decreasing by s]",
            new String[] { "ddd", "bb", "aaa" });
    }

    @Test
    public void testTransformForList() {
        // Transform to should return array
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform to s + s.length()]",
            new String[] { "bb2", "ddd3", "aaa3" });
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform \n     to s + s.length()]",
            new String[] { "bb2", "ddd3", "aaa3" });
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform \n     unique to s + s.length()]",
            new String[] { "bb2", "ddd3", "aaa3" });
        // Transform String array to array of List
        assertEquals("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) transform to Arrays.asList(s)]",
            new List[] { Collections.singletonList("bb"),
                    Collections.singletonList("ddd"),
                    Collections.singletonList("aaa") });
        // Transform List to List of Lists
        assertEquals(
            "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform to Arrays.asList(s)]",
            new List[] { Collections.singletonList("bb"),
                    Collections.singletonList("ddd"),
                    Collections.singletonList("aaa") });
    }

    @Test
    public void testSplitByForList() {
        // Split By should return array of arrays
        assertEquals(
            "List list = new ArrayList(); list.add(\"5000\");list.add( \"2002\");list.add(\"3300\");list.add(\"2113\"); list[(String s) split by substring(0,1)]",
            new String[][] { { "5000" }, { "2002", "2113" }, { "3300" } });

        // Split By when list contains Lists
        assertEquals(
            "List list = new ArrayList(); list.add(Arrays.asList(1));list.add(Arrays.asList(2, 3));list.add(Arrays.asList(4)); list[(List l) split by size()]",
            new List[][] { { Collections.singletonList(1), Collections.singletonList(4) }, { Arrays.asList(2, 3) } });
        // Check array element type. It must be List, not Object (Object does not contain method size()).
        assertEquals(
            "List list = new ArrayList(); list.add(Arrays.asList(1));list.add(Arrays.asList(2, 3));list.add(Arrays.asList(4)); list[(List l) split by size()][0][1].size()",
            1);
    }

    @Test
    public void testArrayOfList() {
        // When array has the type List[] then inside of aggregate function array element type must be List, not Object.
        assertEquals("List[] ary = {Arrays.asList(1), Arrays.asList(2, 3), Arrays.asList(4)}; ary[split by size()]",
            new List[][] { { Collections.singletonList(1), Collections.singletonList(4) }, { Arrays.asList(2, 3) } });
        assertEquals(
            "List[] ary = {Arrays.asList(1), Arrays.asList(2, 3), Arrays.asList(4)}; ary[split \n    by size()]",
            new List[][] { { Collections.singletonList(1), Collections.singletonList(4) }, { Arrays.asList(2, 3) } });
    }

    @Test
    public void testStatic() {
        assertEquals("int.class", int.class);
        assertEquals("Double.class", Double.class);
        assertEquals("String.valueOf(5)", String.valueOf(5));
        assertEquals("Math.PI", Math.PI);
        assertEquals("\"c\".getClass()", "c".getClass());
        assertEquals("\"c\".class", String.class); // getClass()

        assertEquals("'c'.class", char.class);
        assertEquals("1.1.class", double.class);
        assertEquals("10d.class", double.class);
        assertEquals("int x = 5; x.class", int.class);

        assertError("String.length()", CompositeSyntaxNodeException.class);
        assertError("Double.isNaN()", CompositeSyntaxNodeException.class);
        assertError("Double.getClass()", CompositeSyntaxNodeException.class);
        assertError("int.getClass()", CompositeSyntaxNodeException.class);
    }

    @Test
    public void testLongName() {
        assertEquals("new java.math.BigDecimal(10)", new java.math.BigDecimal(10));
        assertEquals("java.io.File f = new java.io.File(\"./temp\"); f.getName()", "temp");
    }

    @Test
    public void testArrayInitializationObject() {
        assertEquals("new Byte[0]", new Byte[0]);
        assertEquals("new Byte[5]", new Byte[5]);
        assertEquals("new Byte[]{1,2,3}", new Byte[] { 1, 2, 3 });
        assertEquals("new Byte[3][]", new Byte[3][]);
        assertEquals("new Byte[3][5]", new Byte[3][5]);
        assertEquals("new Byte[3][5][]", new Byte[3][5][]);
        assertEquals("new Byte[]{}", new Byte[] {});
        assertEquals("new Byte[][]{}", new Byte[][] {});
        assertEquals("new Byte[][]{{},{4}}", new Byte[][] { {}, { 4 } });
        assertEquals("new Byte[][]{{1,2,3},{4}}", new Byte[][] { { 1, 2, 3 }, { 4 } });
        assertEquals("new Byte[]{4}", new Byte[] { 4 });
    }

    @Test
    public void testArrayInitializationPrimitives() {
        assertEquals("new int[0]", new int[0]);
        assertEquals("new int[5]", new int[5]);
        assertEquals("new int[]{1,2,3}", new int[] { 1, 2, 3 });
        assertEquals("new int[3][]", new int[3][]);
        assertEquals("new int[3][5]", new int[3][5]);
        assertEquals("new int[3][5][]", new int[3][5][]);
        assertEquals("new int[]{}", new int[] {});
        assertEquals("new int[][]{}", new int[][] {});
        assertEquals("new int[][]{{},{4}}", new int[][] { {}, { 4 } });
        assertEquals("new int[][]{{1,2,3},{4}}", new int[][] { { 1, 2, 3 }, { 4 } });
        assertEquals("new int[]{4}", new int[] { 4 });
    }
}
