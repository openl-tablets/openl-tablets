package org.openl.ie.constrainer;

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
 * An implementation of a {@link Goal} that prints the solution that is an array
 * of the integer variables.
 */
public class GoalPrintSolution extends GoalImpl {
    private IntExpArray _intvars;
    private int _solution = 0;
    private java.io.PrintStream _out;

    /**
     * Constructor with a given array of expressions. The information will be
     * printed to the Constrainer's output stream.
     */
    public GoalPrintSolution(IntExpArray intvars) {
        this(intvars, intvars.constrainer().out());
    }

    /**
     * Constructor with a given array of expressions and an output stream.
     */
    public GoalPrintSolution(IntExpArray intvars, java.io.PrintStream out) {
        super(intvars.constrainer(), "PrintSolution");
        _intvars = intvars;
        _solution = 0;
        _out = out;
    }

    /**
     * Prints the solution to the output stream.
     */
    public Goal execute() throws Failure {
        _solution++;
        _out.print("\nSolution " + _solution + ": ");
        for (int i = 0; i < _intvars.size(); i++) {
            if (i != 0) {
                _out.print(", ");
            }
            _out.print(_intvars.elementAt(i));
        }
        // _out.println();
        // _constrainer.doPrintInformation();
        return null;
    }
}
