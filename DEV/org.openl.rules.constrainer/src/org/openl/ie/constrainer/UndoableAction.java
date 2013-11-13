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
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;
import org.openl.util.Log;

/**
 * An implementation of the undoable action for the goal.
 *
 * @todo UndoImpl._undoable is not used -> use another "UndoImpl".
 */
class UndoableAction extends UndoImpl {
    static ReusableFactory _factory = new ReusableFactory() {
        @Override
        protected Reusable createNewElement() {
            return new UndoableAction();
        }
    };

    private Goal _goal;

    static UndoableAction getUndo(Goal goal) {
        UndoableAction u = (UndoableAction) _factory.getElement();
        u._goal = goal;
        return u;
    }

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        return "undoable action: " + _goal;
    }

    @Override
    public void undo() {
        try {
            _goal.execute();
        } catch (Failure e) {
            Log.error("Unexpected exception executing undoable action: ", e);
        }
        super.undo();
    }

} // ~UndoableAction
