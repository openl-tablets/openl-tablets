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
 * An implementation of the FloatVarSelector interface that selects the unbound variable with with the smallest domain.
 *
 * @see FloatVarSelector
 * @see FloatVarSelectorFirstUnbound
 * @see FloatVarSelectorMinSizeMin
 * @see GoalFloatGenerate
 */
public class FloatVarSelectorMinSize implements FloatVarSelector {
    private FloatExpArray _vars;

    /**
     * Constructor from the FloatExpArray.
     */
    public FloatVarSelectorMinSize(FloatExpArray vars) {
        _vars = vars;
    }

    /**
     * Selects the unbound variable with the smallest domain. If all variables are bound, returns -1.
     */
    public int select() {
        double min = Double.POSITIVE_INFINITY;
        int min_var = -1;
        int size = _vars.size();

        FloatExp[] vars = _vars.data();

        for (int i = 0; i < size; ++i) {
            FloatExp vari = vars[i];

            double mini = vari.size();

            if (!vari.bound()) {
                if (min > mini || min_var == -1) {
                    min = mini;
                    min_var = i;
                }
            }
        }

        return min_var;
    }

} // ~FloatVarSelectorMinSize
