package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.UndoImpl;
import org.openl.ie.constrainer.Undoable;
import org.openl.ie.constrainer.UndoableInt;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * A generic implementation of the UndoableInt.
 */
// public final class UndoableIntImpl extends UndoableOnceImpl implements
// UndoableInt
public final class UndoableIntImpl extends UndoableImpl implements UndoableInt {
    /**
     * Undo Class for UndoUndoableInt.
     */
    static class UndoUndoableInt extends UndoImpl {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoUndoableInt();
            }

        };

        private int _value;

        static UndoUndoableInt getUndo() {
            return (UndoUndoableInt) _factory.getElement();
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "UndoUndoableInt " + undoable();
        }

        @Override
        public void undo() {
            UndoableIntImpl var = (UndoableIntImpl) undoable();
            var._value = _value;
            super.undo();
        }

        @Override
        public void undoable(Undoable u) {
            super.undoable(u);
            UndoableInt var = (UndoableInt) u;
            _value = var.value();
        }

    } // ~UndoUndoableInt

    private int _value;

    /**
     * Constructor with a given value.
     */
    public UndoableIntImpl(Constrainer constrainer, int value) {
        this(constrainer, value, "");
    }

    /**
     * Constructor with a given value and name.
     */
    public UndoableIntImpl(Constrainer constrainer, int value, String name) {
        super(constrainer, name);
        _value = value;
    }

    @Override
    public Undo createUndo() {
        return UndoUndoableInt.getUndo();
    }

    /**
     * Sets the current value.
     */
    void forceValue(int value) {
        _value = value;
    }

    @Override
    public void setValue(int value) {
        if (value != _value) {
            addUndo();
            _value = value;
        }
    }

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        return name() + "[" + _value + "]";
    }

    @Override
    public int value() {
        return _value;
    }

} // ~UndoableIntImpl
