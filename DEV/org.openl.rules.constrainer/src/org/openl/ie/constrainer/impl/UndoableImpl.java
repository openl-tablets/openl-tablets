package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.ConstrainerObjectImpl;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.Undoable;

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
 * A generic implementation of the Undoable interface.
 */
public abstract class UndoableImpl extends ConstrainerObjectImpl implements Undoable {
    /**
     * Constructor with a given constrainer and name.
     */
    public UndoableImpl(Constrainer c, String name) {
        super(c, name);
    }

    public void addUndo() {
        Undo undo_object = createUndo();
        undo_object.undoable(this);
        // Debug.on();Debug.print("add " + undo_object);Debug.off();
        constrainer().addUndo(undo_object);
    }

    public boolean undone() {
        return false;
    }

    public void undone(boolean b) {
    }

} // ~UndoableImpl
