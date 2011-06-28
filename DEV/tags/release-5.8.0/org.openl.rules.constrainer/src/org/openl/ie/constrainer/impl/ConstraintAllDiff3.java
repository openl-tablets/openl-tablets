package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.ConstraintImpl;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalPrint;
import org.openl.ie.constrainer.GoalPrintObject;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntValueSelector;
import org.openl.ie.constrainer.IntValueSelectorMin;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.IntVarSelector;
import org.openl.ie.constrainer.IntVarSelectorMinSize;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
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
/**
 * An implementation of the constraint "All Different".
 *
 * Any two constrained integer expressions from the vector (parameter of this
 * constraint) should can not be instantiated with the same value.
 */
public final class ConstraintAllDiff3 extends ConstraintImpl {
    // PRIVATE MEMBERS
    private IntExpArray _intvars;

    // TEST
    static void test1(String args[]) throws Exception {
        Constrainer C = new Constrainer("Test");

        IntExp x = C.addIntVar(0, 10, "x", IntVar.DOMAIN_DEFAULT);
        IntExp y = C.addIntVar(2, 10, "y", IntVar.DOMAIN_DEFAULT);
        IntExp z = C.addIntVar(0, 10, "z", IntVar.DOMAIN_DEFAULT);

        int size = 3;
        IntExpArray vars = new IntExpArray(C, size);
        vars.set(x, 0);
        vars.set(y, 1);
        vars.set(z, 2);

        Constraint constraintAllDiff2 = new ConstraintAllDiff2(vars);
        constraintAllDiff2.execute();

        // IntExp cost = C.addIntVar(0,20,"cost");
        // Constraint sum = new ConstraintAddVector(vars,cost);
        // sum.execute();
        IntExp cost = C.sum(vars);
        cost.name("cost");

        // x.lessOrEqual(y).execute();
        x.mul(2).sub(cost).more(y).execute();

        C.traceChoicePoints(vars);
        // C.displayOnBacktrack(vars);
        C.traceFailures(vars);

        IntValueSelector value_selector = new IntValueSelectorMin();
        IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
        Goal print = new GoalAnd(new GoalPrint(vars), new GoalPrintObject(C, cost));
        Goal solution = new GoalAnd(print, new GoalGenerate(vars, var_selector, value_selector), print);

        // if (!C.execute(new GoalMinimize(solution,cost)))
        if (!C.execute(solution)) {
            System.out.println("No solutions");
        }
        System.out.println(x + " " + y + " " + " " + z + cost);
    }

    // ////////////////////////////////////////////// inner class
    // AllDiffObserver
    public ConstraintAllDiff3(IntExpArray intvars) throws Failure {
        super(intvars.constrainer(), "AllDiff");
        _intvars = intvars;

    }

    public Goal execute() throws Failure {

        IntExpArray cards = _intvars.cards().cards();

        for (int i = 0; i < cards.size(); ++i) {
            Constraint ct = cards.elementAt(i).less(2);
            ct.execute();
        }

        return null;
    } // end of execute
} // ~ConstraintAllDiff3
