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

/**
 * An interface for the reusable holder of the undoable object.
 *
 * @see Undoable
 */
public interface Undo extends Reusable, java.io.Serializable {
    /**
     * Restores the state of the undoable object to the state it had just before this undo was created.
     */
    public void undo();

    /**
     * Returns an undoable object for which this undo-object was created.
     */
    public Undoable undoable();

    /**
     * Sets an undoable object for which this undo-object was created.
     */
    public void undoable(Undoable u);

}
