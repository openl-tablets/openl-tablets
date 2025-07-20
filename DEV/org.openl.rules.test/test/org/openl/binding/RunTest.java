package org.openl.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.exception.CompositeOpenlException;

public class RunTest {
    private static Object runExpression(String expression) {
        OpenL openl = OpenL.getInstance();
        return OpenLManager.run(openl, new StringSourceCodeModule(expression, null));
    }

    private static <T> void assertToExpected(String expression, T expected) {
        Object res = runExpression(expression);
        assertTrue(Objects.deepEquals(expected, res));
    }

    private static void assertError(String expr, String expected) {
        try {
            runExpression(expr);
            fail("Non-reachable");
        } catch (CompositeOpenlException t) {
            assertEquals(expected, t.getErrors()[0].getMessage());
        } catch (Exception t) {
            assertEquals(expected, t.getMessage());
        }
    }

    private static void assertErrorStartWith(String expr, String expected) {
        try {
            runExpression(expr);
            fail("Non-reachable");
        } catch (CompositeOpenlException t) {
            assertTrue(t.getErrors()[0].getMessage().startsWith(expected));
        } catch (Exception t) {
            assertTrue(t.getMessage().startsWith(expected));
        }
    }

    @Test
    public void testBig() {
        assertToExpected("Vector x = new Vector(); x.size()", 0);

        assertToExpected("BigDecimal x = 10, y = 20; x > y", false);
        assertToExpected("BigDecimal x = 10, y = 20; x < y", true);
        assertToExpected("BigDecimal x = 10, y = 20; x + y == 30", true);
        assertToExpected("BigDecimal x = 10, y = 20; x + y  - 5", new BigDecimal(25));

        assertToExpected("BigInteger x = 10; BigDecimal y = x; x + x  - 5", new BigInteger("15", 10));
        assertToExpected("BigInteger x = 10; BigDecimal y = x; x == y", true);
        assertToExpected("BigInteger x = 10; BigDecimal y = x; y == x", true);

        assertToExpected("BigInteger x = 10; x != null", true);
        assertToExpected("BigInteger x = 10; null != x", true);

        assertToExpected("BigDecimal x = 10; x != null", true);
        assertToExpected("BigDecimal x = 10; null != x", true);

        assertToExpected("BigInteger x = 10; x == null", false);
        assertToExpected("BigInteger x = 10; null == x", false);

        assertToExpected("BigDecimal x = 10; x == null", false);
        assertToExpected("BigDecimal x = 10; null == x", false);

    }

    @Test
    public void testComparable() {
        assertToExpected("String x = \"abc\";String y = \"abc\"; x < y", false);
        assertToExpected("String x = \"abc\";String y = \"abc\"; x <= y", true);

        assertError("String x = \"abc\";Integer y = 10; x <= y",
                "Operator 'le(java.lang.String, java.lang.Integer)' is not found.");
    }

    @Test
    public void testLong() {
        assertToExpected("long x = 4; x + 5.0", 9.0);
        assertToExpected("long x = 4; x - 5.0", -1.0);
        assertToExpected("long x = 4; x - 5", -1L);
        assertToExpected("Long x = null; x - 5.0", -5.0);

    }

    @Test
    public void testRun() {

        assertToExpected("String $x$y=null; $x$y == null || $x$y.length() < 10", true);

        assertToExpected("String x=null; x == null || x.length() < 10", true);
        assertToExpected("String x=null; x != null && x.length() < 10", false);

        assertToExpected("String x=null; Boolean b = true; b || x.length() < 10", true);
        assertToExpected("String x=null; Boolean b = false; b && x.length() < 10", false);

        assertToExpected("String x=\"abc\"; x == null || x.length() < 10", true);
        assertToExpected("String x=\"abc\"; x != null && x.length() < 10", true);

        assertToExpected("int x = 5; x += 4", 9);
        assertToExpected("DoubleValue d1 = new DoubleValue(5); DoubleValue d2 = new DoubleValue(4); d1 += d2; d1", 9.0);
        assertToExpected("int i=0; for(int j=0; j < 10; ) {i += j;j++;} i", 45);

        // Testing new implementation of s1 == s2 for Strings. To achieve old
        // identity test Strings must be upcasted to Object
        assertToExpected("String a=\"a\"; String b = \"b\"; a + b == a + 'b'", Boolean.TRUE);
        assertToExpected("String a=\"a\"; String b = \"b\"; a + b == a + 'c'", Boolean.FALSE);
        assertToExpected("String a=\"a\"; String b = \"b\"; a + b != a + 'b'", Boolean.FALSE);
        assertToExpected("String a=\"a\"; String b = \"b\"; a + b != a + 'c'", Boolean.TRUE);
        assertToExpected("String a=\"a\"; String b = \"b\"; (Object)(a + b) == (Object)(a + 'b')", Boolean.TRUE);
        assertToExpected("String a=\"a\"; String b = \"b\"; (Object)(a + b) ==== (Object)(a + 'b')", Boolean.FALSE);

        assertToExpected("boolean a=true; boolean b = false; a == !b", Boolean.TRUE);
        assertToExpected("boolean a=true; boolean b = false; a != b", Boolean.TRUE);

        assertToExpected("Integer x = 1; \"aaa\".substring(x)", "aaa".substring(1));

        assertToExpected("int x=5, y=7; x < y ? 'a'+1 : 'b'+1", 'a' + 1);
        assertToExpected("int x=5, y=7; x < y ? 0.7 : 3", 0.7);
        assertToExpected("int x=5, y=7; x >= y ? 3 : 0.7", 0.7);
        assertToExpected("int x=5, y=7; x >= y ? null : 0.7", 0.7);
        assertToExpected("int x=5, y=7; x < y ? 0.7 : null", 0.7);
        assertToExpected("int x=5, y=7; x < y ? 3 : (int)0.7", 3);
        assertToExpected("Number x=new Integer(5);Integer y = 7; 5 < 4 ? x : y", 7);

        assertToExpected("true ? 10 : 20", 10);
        assertToExpected("true ? 10 : 20", 10);
        assertToExpected("false ? 10 : 20", 20);

        assertToExpected("10%", 0.1);
        assertToExpected("10% of \n the  50", 5.0);
        assertToExpected("10% of    the  50", 5.0);

        assertToExpected("5.0 ** 7.0 ", Math.pow(5, 7));
        assertToExpected("DoubleValue x = 5.0; x ** 7 ", Math.pow(5, 7));
        assertToExpected("BigDecimal x = 5.0; x ** 7 ", new BigDecimal("78125.0000000"));

        assertToExpected("1 == 1", true);
        assertToExpected("1 is same as 1", true);
        assertToExpected("1 is same \n as 1", true);
        assertToExpected("1   is   same   as   1", true);
        assertToExpected("1 is the same as 1", true);
        assertToExpected("1 is the \n same as 1", true);
        assertToExpected("1   is   the   same   as   1", true);
        assertToExpected("1 equals to 1", true);
        assertToExpected("1 equals \n to 1", true);
        assertToExpected("1  equals  to  1", true);

        assertToExpected("not false", true);

        assertToExpected("true and true", true);
        assertToExpected("(true)and(true)", true);
        assertToExpected("true and false", false);
        assertToExpected("(true)and(false)", false);
        assertToExpected("true or false", true);
        assertToExpected("(true)or(false)", true);
        assertToExpected("(false)or(false)", false);
        assertToExpected("false or false", false);

        assertToExpected("1 does not equal to 2", true);
        assertToExpected("2 does not \n     equal to 2", false);

        assertToExpected("1 is different from 2", true);
        assertToExpected("2 is different \n  from 2", false);

        assertToExpected("1 is less than 2", true);
        assertToExpected("2 is \n less    than 2", false);

        assertToExpected("2 is more than 1", true);
        assertToExpected("2 is \n more    than 2", false);

        assertToExpected("1 is less or equal 1", true);
        assertToExpected("2 is \n less or   equal 1", false);

        assertToExpected("1 is no more than 1", true);
        assertToExpected("2 is \n no more   than 1", false);

        assertToExpected("1 is in 1", true);
        assertToExpected("1 is \n     in 1", true);

        assertToExpected("1 is more or equal 1", true);
        assertToExpected("1 is \n more or   equal 2", false);

        assertToExpected("1 is no less than 1", true);
        assertToExpected("1 is \n no less     than 2", false);

    }

    @Test
    public void testBusinessLiteral() {
        assertToExpected(" 10M is more or equal 1K", true);
        assertToExpected(" 10K is more or equal 1L", true);
        assertToExpected(" 1K is more or equal 100", true);
        assertToExpected(" -1K is more or equal 100", false);
    }

    @Test
    public void testAggregate() {

        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[2]", "aaa");
        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[!@ startsWith(\"b\")]", "bb");

        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int x = 3; ary[@ length() == x][0]", "ddd");

        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[^@ substring(1,2)][0]", "aaa");
        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[^@ substring(1)][1]", "bb");
        assertToExpected("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[^@ substring(0,1)][0]", "aab");
        assertToExpected("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[v@ substring(0,1)][0]", "ddd");

        assertToExpected("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)].length", 2);
        assertToExpected("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0].length", 3);
        assertToExpected("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][1].length", 1);
        assertToExpected("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0][2]", "aaba");

        assertToExpected("String[] ary = {\"daab\",\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[~@ substring(0,1)][0][1]",
                "ddd");

        assertToExpected("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[*@ substring(0,1)].length", 4);
        assertToExpected("String[] ary = {\"aab\", \"ddd\", \"aac\", \"aaba\"}; ary[*!@ substring(0,1)].length", 2);

        // test named element

        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int x = 3; ary[(String s) @ length() == x][0]", "ddd");
        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) !@ s.startsWith(\"b\")]", "bb");
        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[( s ) !@ s.startsWith(\"aa\")]", "aaa");
        // Index variable in parentheses must not be parsed as aggregate operator
        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; int i = 1; ary[ (i) ]", "ddd");

        assertToExpected(
                "String[] ary = {\"bbcc\", \"dddee\",\"aaabbe\" ,\"aaabb\",}; ary[!@startsWith (ary[( s ) !@ s.toUpperCase().endsWith(\"BB\")])]",
                "aaabbe");

        assertToExpected(
                "String[] ary = {\"bbccdd\", \"dddee\",\"dddeedd\", \"ddd\" ,\"aaabbdd\",}; ary[(s1)!@ s1.startsWith (ary[( s2 ) !@ s2.toUpperCase().startsWith(\"DD\") && s2 != s1])]",
                "dddeedd");

        assertToExpected(
                "String[] ary1 = {\"bbccdd\", \"dddee\",\"dddeedd\", \"ddd\" ,\"aaabbdd\",}; String[] ary2 = {\"ZZZ\", \"XXXX\",\"YYYYYY\", \"ddd\" ,\"aaabbdd\",}; ary1[(s)!@ s.startsWith(\"aa\")] + ary2[(s)!@ s.startsWith(\"ZZ\")]",
                "aaabbddZZZ");

        assertError("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) !@ s.getDay() < 5]",
                "Method 'getDay()' is not found in type 'java.lang.String'.");
        assertError("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(Date d) !@ d.getDay() < 5]",
                "Cannot cast 'java.lang.String' to 'java.util.Date'.");

        assertToExpected("String[] ary = {\"a\", \"b\",\"c\" ,\"a\",\"d\",\"b\",}; ary[(x)~@ x][(str)*@str[0]].length", 4);

        // test lists

        assertToExpected(
                "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list.get(0)",
                "bbccdd");

        assertErrorStartWith(
                "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(Date d)!@ d.getDay() < 6]",
                "java.lang.ClassCastException: Cannot cast java.lang.String to java.util.Date");
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(String s)!@ s.contains(\"ee\")]",
                "dddee");
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bbccdd\"); list.add(\"dddee\");list.add(\"dddeedd\"); list[(String str) select first where str.contains(\"ee\")]",
                "dddee");

        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); int x = 3; list[(String s) @ length() == x][0]",
                "ddd");
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); int x = 3; list[(String s) select all having length() == x][0]",
                "ddd");

        assertToExpected(
                "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) ^@ toLowerCase()][0]",
                "aab");
        assertToExpected(
                "List list = Arrays.asList(\"aab\", \"ddd\", \"aac\", \"aaba\"); list[(String s) v@  substring(0,1)][0]",
                "ddd");

        assertToExpected(
                "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) ~@ substring(0,1)][1][0]",
                "ddd");

        assertToExpected(
                "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) *@ substring(0,1)].length",
                4);
        assertToExpected(
                "List list = new ArrayList(); list.add(\"AABA\"); list.add(\"ddd\"); list.add( \"aac\"); list.add(\"aab\"); list[(String x) *!@ substring(0,1).toLowerCase()].length",
                2);

        // Test spaces
        assertToExpected("String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order by toString() ][0]", "aaba");
        assertToExpected("String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order \n      by toString() ][0]",
                "aaba");
        assertToExpected(
                "String[] ary = {\"z\", \"dd\", \"aac\", \"aaba\"}; ary[ order \n      increasing by toString() ][0]",
                "aaba");
        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[ !@ startsWith(\"d\") ]", "ddd");
    }

    @Test
    public void testSelectAllForList() {
        // Select all should return array
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all having length() == 3]",
                new String[]{"ddd", "aaa"});
        // Demonstrate that no need to cast to String if "select all" returns array instead of List
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add(\"aaaa\"); list[(String a) select all having a.startsWith(\"a\")][0].length()",
                4);
        // Select all with spaces between words
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all \n    having length() == 3]",
                new String[]{"ddd", "aaa"});
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) select all \n     where length() == 3]",
                new String[]{"ddd", "aaa"});
    }

    @Test
    public void testOrderByForList() {
        // Order by should return array
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order by s]",
                new String[]{"aaa", "bb", "ddd"});
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order decreasing by s]",
                new String[]{"ddd", "bb", "aaa"});
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) order \n     decreasing by s]",
                new String[]{"ddd", "bb", "aaa"});
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s)^@s]",
                new String[]{"aaa", "bb", "ddd"});
        assertError("List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[^@s]",
                "Identifier 's' is not found.");
    }

    @Test
    public void testTransformForList() {
        // Transform to should return array
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform to s + s.length()]",
                new String[]{"bb2", "ddd3", "aaa3"});
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform \n     to s + s.length()]",
                new String[]{"bb2", "ddd3", "aaa3"});
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform \n     unique to s + s.length()]",
                new String[]{"bb2", "ddd3", "aaa3"});
        // Transform String array to array of List
        assertToExpected("String[] ary = {\"bb\", \"ddd\", \"aaa\"}; ary[(String s) transform to Arrays.asList(s)]",
                new List[]{Collections.singletonList("bb"),
                        Collections.singletonList("ddd"),
                        Collections.singletonList("aaa")});
        // Transform List to List of Lists
        assertToExpected(
                "List list = new ArrayList(); list.add(\"bb\");list.add( \"ddd\");list.add(\"aaa\"); list[(String s) transform to Arrays.asList(s)]",
                new List[]{Collections.singletonList("bb"),
                        Collections.singletonList("ddd"),
                        Collections.singletonList("aaa")});
    }

    @Test
    public void testSplitByForList() {
        // Split By should return array of arrays
        assertToExpected(
                "List list = new ArrayList(); list.add(\"5000\");list.add( \"2002\");list.add(\"3300\");list.add(\"2113\"); list[(String s) split by substring(0,1)]",
                new String[][]{{"5000"}, {"2002", "2113"}, {"3300"}});

        // Split By when list contains Lists
        assertToExpected(
                "List list = new ArrayList(); list.add(Arrays.asList(1));list.add(Arrays.asList(2, 3));list.add(Arrays.asList(4)); list[(List l) split by size()]",
                new List[][]{{Collections.singletonList(1), Collections.singletonList(4)}, {Arrays.asList(2, 3)}});
        // Check array element type. It must be List, not Object (Object does not contain method size()).
        assertToExpected(
                "List list = new ArrayList(); list.add(Arrays.asList(1));list.add(Arrays.asList(2, 3));list.add(Arrays.asList(4)); list[(List l) split by size()][0][1].size()",
                1);
    }

    @Test
    public void testArrayOfList() {
        // When array has the type List[] then inside of aggregate function array element type must be List, not Object.
        assertToExpected("List[] ary = {Arrays.asList(1), Arrays.asList(2, 3), Arrays.asList(4)}; ary[split by size()]",
                new List[][]{{Collections.singletonList(1), Collections.singletonList(4)}, {Arrays.asList(2, 3)}});
        assertToExpected(
                "List[] ary = {Arrays.asList(1), Arrays.asList(2, 3), Arrays.asList(4)}; ary[split \n    by size()]",
                new List[][]{{Collections.singletonList(1), Collections.singletonList(4)}, {Arrays.asList(2, 3)}});
    }

    @Test
    public void testStatic() {
        assertToExpected("int.class", int.class);
        assertToExpected("Double.class", Double.class);
        assertToExpected("String.valueOf(5)", String.valueOf(5));
        assertToExpected("Math.PI", Math.PI);
        assertToExpected("\"c\".getClass()", "c".getClass());
        assertToExpected("\"c\".class", String.class); // getClass()

        assertToExpected("'c'.class", char.class);
        assertToExpected("1.1.class", double.class);
        assertToExpected("10d.class", double.class);
        assertToExpected("int x = 5; x.class", int.class);

        assertError("String.length()", "Static method 'length()' is not found in type 'java.lang.String'.");
        assertError("Double.isNaN()", "Static method 'isNaN()' is not found in type 'java.lang.Double'.");
        assertError("Double.getClass()", "Static method 'getClass()' is not found in type 'java.lang.Double'.");
        assertError("int.getClass()", "Static method 'getClass()' is not found in type 'int'.");
    }

    @Test
    public void testLongName() {
        assertToExpected("new java.math.BigDecimal(10)", new BigDecimal(10));
        assertToExpected("java.io.File f = new java.io.File(\"./temp\"); f.getName()", "temp");
    }

    @Test
    public void testArrayInitializationObject() {
        assertToExpected("new Byte[0]", new Byte[0]);
        assertToExpected("new Byte[5]", new Byte[5]);
        assertToExpected("new Byte[]{1,2,3}", new Byte[]{1, 2, 3});
        assertToExpected("new Byte[3][]", new Byte[3][]);
        assertToExpected("new Byte[3][5]", new Byte[3][5]);
        assertToExpected("new Byte[3][5][]", new Byte[3][5][]);
        assertToExpected("new Byte[]{}", new Byte[]{});
        assertToExpected("new Byte[][]{}", new Byte[][]{});
        assertToExpected("new Byte[][]{{},{4}}", new Byte[][]{{}, {4}});
        assertToExpected("new Byte[][]{{1,2,3},{4}}", new Byte[][]{{1, 2, 3}, {4}});
        assertToExpected("new Byte[]{4}", new Byte[]{4});
    }

    @Test
    public void testArrayInitializationPrimitives() {
        assertToExpected("new int[0]", new int[0]);
        assertToExpected("new int[5]", new int[5]);
        assertToExpected("new int[]{1,2,3}", new int[]{1, 2, 3});
        assertToExpected("new int[3][]", new int[3][]);
        assertToExpected("new int[3][5]", new int[3][5]);
        assertToExpected("new int[3][5][]", new int[3][5][]);
        assertToExpected("new int[]{}", new int[]{});
        assertToExpected("new int[][]{}", new int[][]{});
        assertToExpected("new int[][]{{},{4}}", new int[][]{{}, {4}});
        assertToExpected("new int[][]{{1,2,3},{4}}", new int[][]{{1, 2, 3}, {4}});
        assertToExpected("new int[]{4}", new int[]{4});
    }
}
