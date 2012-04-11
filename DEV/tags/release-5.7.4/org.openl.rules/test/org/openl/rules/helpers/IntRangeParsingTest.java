/**
 *
 */
package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.syntax.exception.CompositeOpenlException;
import org.openl.syntax.exception.CompositeSyntaxNodeException;

/**
 *
 */
public class IntRangeParsingTest {

    public interface ITestI {
        String hello1(int hour);
    }
    
    @Test
    public void testDollarSymbol() {
        assertEquals(new IntRange(13, 200), new IntRange("$13 - 200"));
        assertEquals(new IntRange(11, 31), new IntRange("[$11; $32)"));
        assertEquals(new IntRange(3, Integer.MAX_VALUE), new IntRange(">$2"));
        assertEquals(new IntRange(10, 10), new IntRange("$10"));
        assertEquals(new IntRange(2, Integer.MAX_VALUE), new IntRange("$2 +"));
    }
    
    @Test
    public void testBrackets(){
        assertEquals(new IntRange(13, 200), new IntRange("[13; 200]"));
        assertEquals(new IntRange(11, 31), new IntRange("(10 .. 32)"));
        assertEquals(new IntRange(3, 4), new IntRange("(2;4]"));
        assertEquals(new IntRange(10, 100), new IntRange("[10 .. 101)"));
        assertEquals(new IntRange(-10, -1), new IntRange("[-10;0)"));
    }

    @Test
    public void testExtraSpacesAndPluses() {
        assertEquals(new IntRange(3, 5), new IntRange("3 - 5"));
        assertEquals(new IntRange(100, Integer.MAX_VALUE), new IntRange(">= 100"));
        assertEquals(new IntRange(2, Integer.MAX_VALUE), new IntRange("2   +"));
    }

    @Test(expected = CompositeSyntaxNodeException.class)
    public void testFailureParse() {
        new IntRange("--1");
    }

    @Test
    public void testJustNumber() {
        assertEquals(new IntRange(37, 37), new IntRange("37"));
    }

    @Test
    public void testKMB() {
        assertEquals(new IntRange(1000000, Integer.MAX_VALUE), new IntRange("1M+"));
        assertEquals(new IntRange(2000000000, 2000000000), new IntRange("2B"));
        assertEquals(new IntRange(1000, 36000000), new IntRange("1K .. 36M"));
        assertEquals(new IntRange(Integer.MIN_VALUE, 24000), new IntRange("<=24K"));
    }

    @Test
    public void testMinMaxFormat() {
        assertEquals(new IntRange(1, 2), new IntRange("1-2"));
        assertEquals(new IntRange(13, 200), new IntRange("13 .. 200"));
    }

    @Test
    public void testMoreLessFormat() {
        assertEquals(new IntRange(Integer.MIN_VALUE, 11), new IntRange("<12"));
        assertEquals(new IntRange(Integer.MIN_VALUE, 7), new IntRange("<=7"));
        assertEquals(new IntRange(3, Integer.MAX_VALUE), new IntRange(">2"));
    }

    @Test
    public void testPlusFormat() {
        assertEquals(new IntRange(0, Integer.MAX_VALUE), new IntRange("0+"));
    }

    @Test
    public void testSignedNumber() {
        assertEquals(new IntRange(-15, -8), new IntRange("-15  - -8"));
        assertEquals(new IntRange(-100, Integer.MAX_VALUE), new IntRange("-100+"));
        assertEquals(new IntRange(3, Integer.MAX_VALUE), new IntRange(">2"));
        assertEquals(new IntRange(-10, -10), new IntRange("-10"));
    }

    @Test
    public void testVerbal() {
        assertEquals(new IntRange(-100, Integer.MAX_VALUE), new IntRange("-100 and more"));
        assertEquals(new IntRange(3, Integer.MAX_VALUE), new IntRange(" more than 2"));
        assertEquals(new IntRange(3, Integer.MAX_VALUE), new IntRange(" more than 2"));
        assertEquals(new IntRange(Integer.MIN_VALUE, -11), new IntRange("less than -10"));
    }
    
    
    @Test
    public void testInvalidIntRangeParsing1() {
        File xlsFile = new File("test/rules/helpers/IntRangeParsing1.xls");
        TestHelper<ITestI> testHelper;

        try {
            testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

            ITestI instance = testHelper.getInstance();
            instance.hello1(10);
        } catch (Exception e) {
            assertTrue(e.toString().contains("IntRangeParsing1.xls?sheet=hello2&cell=C8"));
            assertEquals(CompositeOpenlException.class, e.getClass());
        }
    }
}
