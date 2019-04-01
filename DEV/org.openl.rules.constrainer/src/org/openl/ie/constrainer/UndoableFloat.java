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
 * An interface for the undoable floating-point value.
 *
 * @see Constrainer#addUndoableFloat
 */
public interface UndoableFloat extends Undoable {
    /**
     * Sets the current value.
     */
    void setValue(double value);

    /**
     * Returns the current value.
     */
    double value();

} // ~UndoableFloat
