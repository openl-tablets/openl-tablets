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
 * An implementation of the FloatVarSelector interface that selects the unbound variable with the smallest domain and
 * the minimal minimum.
 *
 * If there are two variables with the same size, it will select the variable with the minimal minimum.
 *
 * @see FloatVarSelector
 * @see FloatVarSelectorFirstUnbound
 * @see FloatVarSelectorMinSize
 * @see GoalGenerate
 */
public class FloatVarSelectorMinSizeMin implements FloatVarSelector {
    private FloatExpArray _vars;

    /**
     * Constructor from the FloatExpArray.
     */
    public FloatVarSelectorMinSizeMin(FloatExpArray vars) {
        _vars = vars;
    }

    /**
     * Selects the unbound variable with the smallest domain. If there are two variables with the same size, it will
     * select the variable with the minimal minimum. If all variables are bound, returns -1.
     */
    @Override
    public int select() {
        double min_size = Double.POSITIVE_INFINITY;
        double min_min = Double.POSITIVE_INFINITY;
        int min_index = -1;
        int size = _vars.size();

        FloatExp[] vars = _vars.data();

        for (int i = 0; i < size; i++) {
            FloatExp vari = vars[i];
            if (!vari.bound()) {
                if (min_index == -1 || min_size >= vari.size()) {
                    if (min_size > vari.size() || min_min > vari.min()) {
                        min_size = vari.size();
                        min_min = vari.min();
                        min_index = i;
                    }
                }
            }
        }
        return min_index;
    }

} // ~FloatVarSelectorMinSizeMin
