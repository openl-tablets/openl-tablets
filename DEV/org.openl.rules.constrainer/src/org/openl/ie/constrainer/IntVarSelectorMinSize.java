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
 * An implementation of the IntVarSelector interface that selects the unbound variable with with the smallest domain.
 *
 * @see IntVarSelector
 * @see IntVarSelectorFirstUnbound
 * @see IntVarSelectorMinSizeMin
 * @see GoalGenerate
 */
public class IntVarSelectorMinSize implements IntVarSelector {
    private IntExpArray _intvars;

    /**
     * Constructor from the IntExpArray.
     */
    public IntVarSelectorMinSize(IntExpArray intvars) {
        _intvars = intvars;
    }

    /**
     * Selects the unbound variable with the smallest domain. If all variables are bound, returns -1.
     */
    @Override
    public int select() {
        int min = Integer.MAX_VALUE;
        int min_var = -1;
        int size = _intvars.size();

        IntExp[] vars = _intvars.data();

        for (int i = 0; i < size; ++i) {
            IntExp vari = vars[i];

            int mini = vari.size();

            if (mini != 1) {
                if (min > mini || min_var == -1) {
                    min = mini;
                    min_var = i;
                    if (min == 2) {
                        break;
                    }
                }
            }
        }
        return min_var;
    }

} // ~IntVarSelectorMinSize
