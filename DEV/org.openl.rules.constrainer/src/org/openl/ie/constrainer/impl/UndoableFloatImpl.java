package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * A generic implementation of the UndoableFloat.
 */
// public final class UndoableFloatImpl extends UndoableOnceImpl implements
// UndoableFloat
public final class UndoableFloatImpl extends UndoableOnceImpl implements UndoableFloat {
    /**
     * Undo Class for UndoUndoableFloat.
     */
    static class UndoUndoableFloat extends UndoImpl {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoUndoableFloat();
            }

        };

        private double _value;

        static UndoUndoableFloat getUndo() {
            return (UndoUndoableFloat) _factory.getElement();
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "UndoUndoableFloat " + undoable();
        }

        @Override
        public void undo() {
            UndoableFloatImpl var = (UndoableFloatImpl) undoable();
            var._value = _value;
            super.undo();
        }

        @Override
        public void undoable(Undoable u) {
            super.undoable(u);
            UndoableFloat var = (UndoableFloat) u;
            _value = var.value();
        }

    } // ~UndoUndoableFloat

    private double _value;

    /**
     * Constructor with a given value.
     */
    public UndoableFloatImpl(Constrainer constrainer, double value) {
        this(constrainer, value, "");
    }

    /**
     * Constructor with a given value and name.
     */
    public UndoableFloatImpl(Constrainer constrainer, double value, String name) {
        super(constrainer, name);
        _value = value;
    }

    @Override
    public Undo createUndo() {
        return UndoUndoableFloat.getUndo();
    }

    /**
     * Sets the current value.
     */
    void forceValue(double value) {
        _value = value;
    }

    @Override
    public void setValue(double value) {
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
    public double value() {
        return _value;
    }

} // ~UndoableFloatImpl
