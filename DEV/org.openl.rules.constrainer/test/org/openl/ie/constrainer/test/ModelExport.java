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
package org.openl.ie.constrainer.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.openl.ie.constrainer.ConstrainerObject;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.Subject;

/**
 * Utilities for the export of the Constrainer model/problem. Experimental version for ILOG Solver. Uses the names of
 * the expressions as their export presentation (???).
 */
public class ModelExport {
    IntExpArray _vars;
    IntExp _cost;
    IntExpArray _violations;
    String _sumViolationsName = "sumViolations";
    String _constraintMethodName = "createConstraint";

    static Collection getDependentConstraints(IntExpArray arr) {
        Collection result = new HashSet();
        for (int i = 0; i < arr.size(); i++) {
            result.addAll(getDependentConstraints(arr.elementAt(i)));
        }
        return result;
    }

    static Collection getDependentConstraints(Subject s) {

        Vector v = new Vector();

        for (Iterator iter = s.allDependents().iterator(); iter.hasNext();) {
            Object element = iter.next();
            if (element instanceof Constraint) {
                v.add(element);

            }

        }
        return v;

    }

    static void printConstraintCalls(String methodName, Collection constraints, java.io.PrintStream out) {
        Iterator it = constraints.iterator();
        int i = 0;
        while (it.hasNext()) {
            i++;
            out.println("_m.add(" + methodName + i + "());");
        }
    }

    static void printConstraintMethods(String methodName, Collection constraints, java.io.PrintStream out) {
        Iterator it = constraints.iterator();
        int i = 0;
        while (it.hasNext()) {
            i++;
            ConstrainerObject c = (ConstrainerObject) it.next();
            out.println("IlcConstraint " + methodName + i + "()");
            out.println("{");
            out.println("return " + c.name() + ";");
            out.println("}");
        }
    }

    static void printSum(String sumName, IntExpArray arr, java.io.PrintStream out) {
        out.println("IlcIntExp " + sumName + "=0;");
        for (int i = 0; i < arr.size(); ++i) {
            IntExp exp = arr.elementAt(i);
            out.println("{ IlcIntExp exp =" + exp.name() + ";");
            // out.println(sumName + "=" + sumName + "+" + exp.name() + ";");
            out.println(sumName + "=" + sumName + "+exp;");
            out.println("}");
        }
    }

    static void printVar(IntExp v, java.io.PrintStream out) {
        out.println("IlcIntVar " + v.name() + "(m," + v.min() + "," + v.max() + ",\"" + v.name() + "\");");
    }

    static void printVarArrayMethod(String methodName, IntExpArray arr, java.io.PrintStream out) {
        int min = -100, max = 1000;
        out.println("IlcIntVarArray " + methodName + "()");
        out.println("{");
        out.println("IlcIntVarArray vars(m," + arr.size() + "," + min + "," + max + ");");
        for (int i = 0; i < arr.size(); i++) {
            IntExp v = arr.elementAt(i);
            out.println("vars[" + i + "]=" + v.name() + ";");
        }
        out.println("return vars;");
        out.println("}");
    }

    static void printVarMembers(IntExpArray arr, java.io.PrintStream out) {
        for (int i = 0; i < arr.size(); i++) {
            IntExp v = arr.elementAt(i);
            out.println("IlcIntVar " + v.name() + "(m," + v.min() + "," + v.max() + ",\"" + v.name() + "\");");
            out.println("vars[" + i + "]=" + v.name() + ";");
        }
    }

    public ModelExport(IntExpArray vars, IntExp cost, IntExpArray violations) {
        _vars = vars;
        _cost = cost;
        _violations = violations;
    }

    public void export(String className, java.io.PrintStream out) throws Exception {
        // Constrainer constrainer = vars.constrainer();

        // class
        out.println("class " + className);
        out.println("{");
        out.println("public:");

        out.println("IlcManager& _m;");

        Collection constraints = getDependentConstraints(_vars);

        printVarMembers(_vars, out);

        printConstraintMethods(_constraintMethodName, constraints, out);

        printVarArrayMethod("createViolations", _vars, out);

        // printSum("sumViolations",violations,out);
        // printVar(cost,out);
        // out.println("_m.add(sumViolations==cost);");

        // ctor
        out.println("" + className + "(IlcManager& m)");
        out.println(": _m(m)");
        out.println("{");

        printConstraintCalls(_constraintMethodName, constraints, out);

        // ~ctor
        out.println("}");

        // ~class
        out.println("}");
    }

    public void export(String className, String fileName) throws Exception {
        java.io.PrintStream out = new java.io.PrintStream(new java.io.FileOutputStream(fileName));
        export(className, out);
        out.close();
    }

}
