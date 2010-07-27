///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000, 2001
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalFloatFastMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;

import junit.framework.Assert;
import junit.framework.TestCase;

public class GoalFloatFastMinimizeTest extends TestCase {

    Constrainer c;

    public GoalFloatFastMinimizeTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        c = new Constrainer("GoalFloatFastMinimizeTest");
    }

    @Override
    protected void tearDown() throws Exception {
    }

    /**
     * Minimize -costStep * (x+y), for positive costStep, x, and y. Minimum is
     * at (xMax,yMax).
     */
    public void testSimple() throws Failure {
        final int xMax = 10, yMax = 10;
        final double costStep = 0.1;

        IntExp x = c.addIntVar(0, xMax, "x");
        IntExp y = c.addIntVar(0, yMax, "y");
        IntExpArray vars = new IntExpArray(c, x, y);

        FloatExp cost = x.add(y).mul(costStep).neg();

        Goal goalGenerate = new GoalGenerate(vars);
        Goal goalMinimize = new GoalFloatFastMinimize(goalGenerate, cost, costStep / 2);

        c.execute(goalMinimize);

        Assert.assertEquals("x", xMax, x.value());
        Assert.assertEquals("y", yMax, y.value());

        Assert.assertEquals("cost", -costStep * (xMax + yMax), cost.value(), 1e-6);
    }

}
