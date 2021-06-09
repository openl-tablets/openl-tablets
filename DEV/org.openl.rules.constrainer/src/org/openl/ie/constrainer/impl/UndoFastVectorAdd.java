package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.UndoImpl;
import org.openl.ie.tools.FastVector;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * An implementation of the Undo for FastVector.add().
 *
 * @see Undo
 */
public class UndoFastVectorAdd extends UndoImpl implements java.io.Serializable {

    static final ReusableFactory _factory = new ReusableFactory() {
        @Override
        protected Reusable createNewElement() {
            return new UndoFastVectorAdd();
        }

    };

    private FastVector _v;

    static public UndoFastVectorAdd getUndo(FastVector v) {
        UndoFastVectorAdd undo = (UndoFastVectorAdd) _factory.getElement();
        undo._v = v;
        return undo;
    }

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        return "UndoFastVectorAdd " + _v;
    }

    @Override
    public void undo() {
        _v.removeElementAt(_v.size() - 1);
        super.undo();
    }

}