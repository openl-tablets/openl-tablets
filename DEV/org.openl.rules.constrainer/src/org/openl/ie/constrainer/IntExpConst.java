package org.openl.ie.constrainer;

import org.openl.ie.constrainer.impl.ConstraintExpEqualsValue;
import org.openl.ie.constrainer.impl.IntExpImpl;

//
//: IntExpConst.java
//
/**
 * An implementation of the constant integer expression. Many methods from IntExpImpl are overloaded with optimized
 * implementation.
 */
public class IntExpConst extends IntExpImpl {
    protected final int _const;

    public IntExpConst(Constrainer constrainer, int c) {
        super(constrainer);
        if (constrainer().showInternalNames()) {
            _name = Integer.toString(c);
        }

        _const = c;
    }

    /**
     * @return <code>(IntExp)(value+const)</code>
     */
    @Override
    final public IntExp add(int value) {
        // return new IntExpConst(constrainer(),_const + value);
        return getIntExp(IntExpConst.class, _const + value);
    }

    /**
     * Overrides the appropriate method of IntExpImpl
     *
     * @return <code>(IntExp)(exp + const)</code>
     */
    @Override
    final public IntExp add(IntExp exp) {
        return exp.add(_const);
    }

    /**
     * @return true
     */
    @Override
    final public boolean bound() {
        return true;
    }

    /**
     * @param value The value to be checked
     * @return (value == const)
     */
    @Override
    final public boolean contains(int value) {
        return value == _const;
    }

    /**
     * @param value The value the IntExpConst must be equal to
     * @return <code> ConstraintConst(value == const)</code>
     */
    @Override
    final public Constraint equals(int value) // this = value
    {
        return new ConstraintConst(constrainer(), value == _const);
    }

    /**
     * @param exp IntExp that must be equal to IntExpConst's value.
     * @return <code> ConstraintConst(exp == const)</code>
     */
    @Override
    final public Constraint equals(IntExp exp) // this == exp
    {
        return new ConstraintExpEqualsValue(exp, _const);
    }

    public boolean isInteger() {
        return true;
    }

    @Override
    public boolean isLinear() {
        return true;
    }

    /**
     * Overrides the appropriate method of IntExpImpl
     *
     * @return The value of expression e.g. const
     */
    @Override
    final public int max() {
        return _const;
    }

    /**
     * Overrides the appropriate method of IntExpImpl
     *
     * @return The value of expression
     */
    @Override
    final public int min() {
        return _const;
    }

    /**
     * Do nothig in this release
     *
     */

    @Override
    final public void propagate() throws Failure {
    }

    /**
     *
     * @param value the value to be compared with "const"
     * @throws Failure if value is equal to "const". Do nothing otherwise
     */
    @Override
    final public void removeValue(int value) throws Failure {
        if (value == _const) {
            constrainer().fail("remove const");
        }
    }

    /**
     * Checks wether the value is greater then or equal to "const". If it is not throws Failure.
     *
     * @param max the value to be checked
     */
    @Override
    final public void setMax(int max) throws Failure {
        if (max < _const) {
            constrainer().fail("max<const");
        }
    }

    /**
     * Checks wether the value is less then or equal to "const". If it is not throws Failure
     *
     * @param min the value to be checked
     */
    @Override
    final public void setMin(int min) throws Failure {
        if (min > _const) {
            constrainer().fail("min>const");
        }
    }

    /**
     * Actually it checks wether the value is equal to "const". If it is not throws Failure.
     *
     * @param value The value to be checked
     */
    @Override
    final public void setValue(int value) throws Failure {
        if (value != _const) {
            constrainer().fail("value!=const");
        }
    }

    /**
     * @return 1
     */
    @Override
    final public int size() {
        return 1;
    }

    /**
     *
     * @return "const"
     */
    @Override
    final public int value() {
        return _const;
    }

} // ~IntExpConst
