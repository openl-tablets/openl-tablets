package org.openl.ie.constrainer;

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
    public final IntExp add(int value) {
        return getIntExp(IntExpConst.class, _const + value);
    }

    /**
     * Overrides the appropriate method of IntExpImpl
     *
     * @return <code>(IntExp)(exp + const)</code>
     */
    @Override
    public final IntExp add(IntExp exp) {
        return exp.add(_const);
    }

    /**
     * @return true
     */
    @Override
    public final boolean bound() {
        return true;
    }

    /**
     * @param value The value to be checked
     * @return (value = = const)
     */
    @Override
    public final boolean contains(int value) {
        return value == _const;
    }

    /**
     * @param value The value the IntExpConst must be equal to
     * @return <code> ConstraintConst(value == const)</code>
     */
    @Override
    public final Constraint equalTo(int value) // this = value
    {
        return new ConstraintConst(constrainer(), value == _const);
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
    public final int max() {
        return _const;
    }

    /**
     * Overrides the appropriate method of IntExpImpl
     *
     * @return The value of expression
     */
    @Override
    public final int min() {
        return _const;
    }

    /**
     * Do nothig in this release
     */

    @Override
    public final void propagate() throws Failure {
    }

    /**
     * @param value the value to be compared with "const"
     * @throws Failure if value is equal to "const". Do nothing otherwise
     */
    @Override
    public final void removeValue(int value) throws Failure {
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
    public final void setMax(int max) throws Failure {
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
    public final void setMin(int min) throws Failure {
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
    public final void setValue(int value) throws Failure {
        if (value != _const) {
            constrainer().fail("value!=const");
        }
    }

    /**
     * @return 1
     */
    @Override
    public final int size() {
        return 1;
    }

    /**
     * @return "const"
     */
    @Override
    public final int value() {
        return _const;
    }

} // ~IntExpConst
