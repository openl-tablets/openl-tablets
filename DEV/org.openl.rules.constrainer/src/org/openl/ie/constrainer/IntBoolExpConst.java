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
 * IntBoolExp wraps a boolean value (that is reffered to as "boolean_const" in this documentation) into object that is
 * to be used like constant expression of IntBoolExp type.
 */
public class IntBoolExpConst extends IntExpConst implements IntBoolExp {
    /**
     * Acts like a following constructor: <code>new IntBoolExpConst(c,value)</code>
     */
    public static IntBoolExpConst getIntBoolExpConst(Constrainer c, boolean value) {
        // return new IntBoolExpConst(constrainer(),value);
        return (IntBoolExpConst) c.expressionFactory()
            .getExpression(IntBoolExpConst.class,
                new Object[] { c, new Boolean(value) },
                new Class[] { Constrainer.class, boolean.class });
    }

    /**
     * Constructs an IntBoolExpConst and initializes "boolean_const" (Wrapped boolean value) with value.
     */
    public IntBoolExpConst(Constrainer c, boolean value) {
        super(c, value ? 1 : 0);
    }

    /**
     * @return (IntBoolExp)(<code>isTrue() ? getIntBoolExpConst(constrainer(),value) : this</code>)
     * @see #getIntBoolExpConst(Constrainer, boolean)
     * @see #isTrue()
     */
    final public IntBoolExp and(boolean value) {
        return isTrue() ? (IntBoolExp) getIntBoolExpConst(constrainer(), value) : this;
    }

    /**
     * @return (IntBoolExp)(<code>isTrue() ? exp : this</code>)
     * @see #isTrue()
     */
    final public IntBoolExp and(IntBoolExp exp) {
        return isTrue() ? exp : this;
    }

    /**
     *
     * @return (Constraint)(isTrue())
     */
    final public Constraint asConstraint() {
        return new ConstraintConst(constrainer(), isTrue());
    }

    /**
     *
     * @return (boolean_const == 0)
     */
    final public boolean isFalse() {
        return _const == 0;
    }

    /**
     *
     * @return (boolean_const == 0)
     */
    final public boolean isTrue() {
        return _const == 1;
    }

    /**
     * @return (IntBoolExp)(<code>isTrue() ? this : getIntBoolExpConst(constrainer(),value)</code>)
     * @see #getIntBoolExpConst(Constrainer, boolean)
     * @see #isTrue()
     */
    final public IntBoolExp or(boolean value) {
        return isTrue() ? (IntBoolExp) this : getIntBoolExpConst(constrainer(), value);
    }

    /**
     *
     * @return (IntBoolExp)(<code>isTrue() ? this : exp</code>)
     */
    final public IntBoolExp or(IntBoolExp exp) {
        return isTrue() ? this : exp;
    }

    /**
     *
     * @throws Failure if "boolean_const" is equal to <code>true</code>
     */
    final public void setFalse() throws Failure {
        setMax(0);
    }

    /**
     *
     * @throws Failure if "boolean_const" is equal to <code>false</code>
     */
    final public void setTrue() throws Failure {
        setMin(1);
    }

} // ~IntBoolExpImpl
