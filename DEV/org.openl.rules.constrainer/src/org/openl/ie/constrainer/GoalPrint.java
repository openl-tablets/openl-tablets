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
//import java.util.Vector;
/**
 * An implementation of a {@link Goal} that prints an array of IntExp's.
 */
public class GoalPrint extends GoalImpl {
    private IntExpArray _intvars;
    private String _text;
    private boolean _newline;

    /**
     * Creates a goal. Calls <code>GoalPrint(intvars,"\nVariables");</code>
     *
     * @param intvars An array to be printed out.
     * @see #GoalPrint(IntExpArray, String)
     */
    public GoalPrint(IntExpArray intvars) {
        this(intvars, "\nVariables");
    }

    /**
     *
     * @param intvars An array to be printed out
     * @param newline The boolean flag. If "newline"==<code>true</code> then
     *            each element of intvars will be printed starting with a new
     *            line.
     */
    public GoalPrint(IntExpArray intvars, boolean newline) {
        this(intvars, "\nVariables");
        _newline = newline;
    }

    /**
     * Creates a goal.
     *
     * @param intvars An array to be printed out.
     * @param text The leading string of text To begin with, execute method
     *            invokes the following line of code:
     *            <code>System.out.println(text+"("+intvars.size()+"):");</code>
     */
    public GoalPrint(IntExpArray intvars, String text) {
        super((intvars.elementAt(0)).constrainer(), "Print");
        _intvars = intvars;
        _text = text;
        _newline = false;
    }

    /**
     *
     * @param intvars An array to be printed out
     * @param text The leading string of text To begin with, execute method
     *            invokes the following line of code:
     *            <code>System.out.println(text+"("+intvars.size()+"):");</code>
     *
     * @param newline The boolean flag. If "newline"==<code>true</code> then
     *            each element of intvars will be printed starting with a new
     *            line.
     */
    public GoalPrint(IntExpArray intvars, String text, boolean newline) {
        this(intvars, text);
        _newline = newline;
    }

    /**
     * Prints the array to the output stream.
     */
    public Goal execute() throws Failure {
        constrainer().out().println(_text + "(" + _intvars.size() + "):");
        for (int i = 0; i < _intvars.size(); i++) {
            IntExp var = _intvars.elementAt(i);
            if (!_newline) {
                constrainer().out().print(" " + var);
                if (i < _intvars.size() - 1) {
                    constrainer().out().print(",");
                }
            } else {
                constrainer().out().println(var);
            }
        }
        // System.out.print("\n");
        return null;
    }
}
