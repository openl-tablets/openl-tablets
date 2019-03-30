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
 * An interface for the undoable object.
 */
public interface Undoable extends ConstrainerObject {
    /**
     * Creates Undo-object for this undoable object and adds it to the constrainer's undo-stack.
     */
    public void addUndo();

    /**
     * Creates Undo-object for this undoable object.
     */
    public Undo createUndo();

    // /**
    // *
    // */
    // public void restored();

    /**
     * Returns true if undo operation was performed.
     */
    public boolean undone();

    /**
     * Sets 'undone' state for this undoable object.
     */
    public void undone(boolean b);

} // ~Undoable
