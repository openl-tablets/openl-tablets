package com.exigen.ie.constrainer.impl;

import com.exigen.ie.constrainer.Undo;
import com.exigen.ie.constrainer.UndoImpl;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the Undo interface for the UndoableBits.
 *
 * @see Undo
 * @see UndoableBits
 */
public final class UndoBits extends UndoImpl {

    static ReusableFactory _factory = new ReusableFactory() {
        @Override
        protected Reusable createNewElement() {
            return new UndoBits();
        }

    };

    private int _value;

    static UndoBits getUndo(UndoableBits oldbits, int value) {
        UndoBits undo = (UndoBits) _factory.getElement();
        undo.init(oldbits, value);
        return undo;
    }

    /**
     * Constructor for UndoBits.
     */
    void init(UndoableBits oldbits, int value) {
        // UndoableBits new_bits = new UndoableBits(oldbits);
        // undoable(new_bits);
        // undone(oldbits.undone());
        undoable(oldbits);
        _value = value;
    }

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        return "UndoBits " + undoable();
    }

    /**
     * Executes undo() operation for this UndoIntVar object.
     */
    @Override
    public void undo() {
        try {
            UndoableBits oldbits = (UndoableBits) undoable();
            oldbits.bit(_value, false);
            // super.undo();
            // Debug.on();Debug.print("after undo: "+ct.bits());Debug.off();
        } catch (Exception e) {
            System.out.println("UNEXPECTED EXCEPTION during undo for " + this + ": " + e);
        }

    }

} // ~UndoBits

