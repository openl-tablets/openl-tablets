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
 * An implementation of the FloatVarSelector interface that selects the first unbound variable.
 *
 * @see FloatVarSelector
 * @see FloatVarSelectorMinSize
 * @see FloatVarSelectorMinSizeMin
 * @see GoalFloatGenerate
 */
public class FloatVarSelectorFirstUnbound implements FloatVarSelector {
    private FloatExpArray _vars;

    /**
     * Constructor from the FloatExpArray.
     */
    public FloatVarSelectorFirstUnbound(FloatExpArray vars) {
        _vars = vars;
    }

    /**
     * Selects the first unbound variables in the vector of the variables.
     */
    @Override
    public int select() {
        int size = _vars.size();
        for (int i = 0; i < size; i++) {
            FloatExp vari = _vars.elementAt(i);
            if (!vari.bound()) {
                return i;
            }
        }
        return -1;
    }

} // ~FloatVarSelectorFirstUnbound
