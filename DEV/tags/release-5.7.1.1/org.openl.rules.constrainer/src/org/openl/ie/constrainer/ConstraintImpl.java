package org.openl.ie.constrainer;

import org.openl.util.Log;

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
 * A generic abstract implementation of the Constraint interface.
 * <p>
 * Any specific subclass of the ConstraintImpl should implement:
 * <ul>
 * <li>A special constructor that saves the variables constrained by this
 * constraint.
 * <li>The method {@link #execute()}.
 * <li>The method {@link #opposite()} (if it is possible :).
 * </ul>
 *
 * @see Constraint
 * @see Goal
 * @see Observer
 */
public abstract class ConstraintImpl extends GoalImpl implements Constraint {
    /**
     * Constructor with a given constrainer.
     */
    public ConstraintImpl(Constrainer c) {
        this(c, "Constraint");
    }

    /**
     * Constructor with a given constrainer and name.
     */
    public ConstraintImpl(Constrainer c, String name) {
        super(c, name);
        // c.addConstraint(this);
    }

    public Constraint and(Constraint constraint) {
        class ConstraintAndConstraint extends ConstraintImpl {
            private Constraint _c1, _c2;

            ConstraintAndConstraint(Constraint c1, Constraint c2) {
                super(c1.constrainer());
                _c1 = c1;
                _c2 = c2;

                if (constrainer().showInternalNames()) {
                    _name = "(" + c1.name() + "&&" + c2.name() + ")";
                }
            }

            /**
             * @todo there
             */
            public Goal execute() throws Failure {
                _c1.execute();
                _c2.execute();
                return null;
            }
        }

        return new ConstraintAndConstraint(this, constraint);
    }

    public boolean isLinear() {
        return false;
    }

    /**
     * Returns a constraint: <code>(NOT this)</code>. An opposite constraint
     * has semantically an opposite meaning to this constraint.
     * <p>
     * <b>Note</b>: this method simple prints an alert message and returns
     * null. It should be redefined in the subclass of the ConstraintImpl when:
     * <ul>
     * <li>The constraint could be involved in the "ifThen" and "ifThenElse"
     * constraints.
     * <li>The constraint could be involved in the boolean expression.
     * </ul>
     */
    public Constraint opposite() {
        Log.error("Method opposite() has not been defined for " + this);
        return null;
    }

    public void post() throws Failure {
        // execute();
        // _constrainer.propagate();
        _constrainer.postConstraint(this);
    }

    // public Constraint or(Constraint constraint)
    // {
    // class ConstraintOrConstraint extends ConstraintImpl
    // {
    // private Constraint _c1, _c2;
    //
    // ConstraintOrConstraint(Constraint c1, Constraint c2)
    // {
    // super(c1.constrainer());
    // _c1 = c1;
    // _c2 = c2;
    //
    // if(constrainer().showInternalNames())
    // {
    // _name = "("+c1.name()+"||"+c2.name()+")";
    // }
    // }
    //
    // /**
    // * @todo should be implemented not as a GoalOr.
    // */
    // public Goal execute() throws Failure
    // {
    // return new GoalOr(_c1,_c2);
    // }
    // }
    // return new ConstraintOrConstraint(this,constraint);
    // }
    //
    // public Constraint ifThen(Constraint constraint) // if this, then
    // constraint
    // {
    // if (opposite()==null)
    // {
    // System.out.println("Constraint::ifThen: Not defined opposite constraint
    // for "+this);
    // }
    // return opposite().or(constraint); // careful!!: opposite may not be
    // defined
    // }
    //
    // public Constraint ifThenElse(Constraint constraint1, Constraint
    // constraint2)
    // {
    // if (opposite()==null)
    // {
    // System.out.println("Constraint::ifThenElse: Not defined opposite
    // constraint for "+this);
    // }
    // Constraint first_if = opposite().or(constraint1); // careful!!: opposite
    // may not be defined
    // Constraint second_if = this.or(constraint2);
    // return first_if.and(second_if);
    // }
    public IntBoolExp toIntBoolExp() {
        return null;
    }
} // ~ConstraintImpl
