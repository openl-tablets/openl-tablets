package org.openl.ie.constrainer.impl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author
 * @version 1.0
 */
import org.openl.ie.constrainer.impl.FloatCalc;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestFloatCalc extends TestCase {
    private double[] temp = new double[] { 1.0,
            2.123,
            Math.E,
            Math.PI,
            Math.sqrt(10),
            5.125,
            Math.PI * Math.E,
            Math.pow(Math.PI, Math.E),
            Math.pow(Math.E, Math.PI) };
    protected double[] ascendant = null;
    protected double[] descendant = null;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(new TestSuite(TestFloatCalc.class));
    }

    public TestFloatCalc(String name) {
        super(name);
    }

    @Override
    public void setUp() {
        double[] temp = new double[] { 1.0,
                2.123,
                Math.E,
                Math.PI,
                Math.sqrt(10),
                5.125,
                Math.PI * Math.E,
                Math.pow(Math.PI, Math.E),
                Math.pow(Math.E, Math.PI) };

        ascendant = new double[temp.length * 2 + 1];
        descendant = new double[temp.length * 2 + 1];

        ascendant[temp.length] = 0.01;
        descendant[temp.length] = -0.01;

        for (int i = 0; i < temp.length; i++) {
            ascendant[i] = -temp[temp.length - 1 - i];
            ascendant[temp.length + i + 1] = temp[i];
            descendant[i] = -ascendant[i];
            descendant[temp.length + i + 1] = -ascendant[temp.length + i + 1];
        }
    }

    public void testSelf() {
        for (int i = 0; i < ascendant.length - 1; i++) {
            assertTrue(ascendant[i] < ascendant[i + 1]);
            assertTrue(descendant[i] > descendant[i + 1]);
        }
    }

    public void testSqrMin() {
        double result, min, max;
        for (int i = 0; i < ascendant.length - 1; i++) {
            result = FloatCalc.sqrMin(ascendant[i], ascendant[i + 1]);
            min = ascendant[i];
            max = ascendant[i + 1];
            assertTrue("sqrMin(" + min + "," + max + ")= " + result + ">" + min + "*" + min, result <= min * min);
            assertTrue("sqrMin(" + min + "," + max + ")= " + result + ">" + max + "*" + max, result <= max * max);
            min = descendant[i + 1];
            max = descendant[i];
            result = FloatCalc.sqrMin(min, max);
            assertTrue("sqrMin(" + max + "," + min + ")= " + result + ">" + max + "*" + max, result <= max * max);
            assertTrue("sqrMin(" + max + "," + min + ")= " + result + ">" + min + "*" + min,
                result <= descendant[i + 1] * descendant[i + 1]);
        }
        result = FloatCalc.sqrMin(-1, 1);
        assertEquals("sqrMin(-1,1) = " + result, 0, result, Double.MIN_VALUE);
    }

}