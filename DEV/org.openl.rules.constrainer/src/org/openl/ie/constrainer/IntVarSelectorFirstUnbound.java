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
 * An implementation of the IntVarSelector interface that selects the first unbound variable.
 *
 * @see IntVarSelector
 * @see IntVarSelectorMinSize
 * @see IntVarSelectorMinSizeMin
 * @see GoalGenerate
 */
public class IntVarSelectorFirstUnbound implements IntVarSelector {
    private IntExpArray _intvars;

    /**
     * Constructor from the IntExpArray.
     */
    public IntVarSelectorFirstUnbound(IntExpArray intvars) {
        _intvars = intvars;
    }

    /**
     * Selects the first unbound variables in the vector _intvars.
     */
    public int select() {
        int size = _intvars.size();
        for (int i = 0; i < size; i++) {
            IntVar vari = (IntVar) _intvars.elementAt(i);
            if (!vari.bound()) {
                return i;
            }
        }
        return -1;
    }

} // ~IntVarSelectorFirstUnbound
