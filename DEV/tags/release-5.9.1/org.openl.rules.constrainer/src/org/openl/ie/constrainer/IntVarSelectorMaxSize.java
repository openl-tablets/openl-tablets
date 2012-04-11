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
 * An implementation of the IntVarSelector interface that selects the unbound
 * variable with with the smallest domain.
 *
 * @see IntVarSelector
 * @see IntVarSelectorFirstUnbound
 * @see GoalGenerate
 */
public class IntVarSelectorMaxSize implements IntVarSelector {
    private IntExpArray _intvars;

    /**
     * Constructor from the IntExpArray.
     */
    public IntVarSelectorMaxSize(IntExpArray intvars) {
        _intvars = intvars;
    }

    /**
     * Selects the unbound variable with the smallest domain. If all variables
     * are bound, returns -1.
     */
    public int select() {
        int max = Integer.MIN_VALUE;
        int max_var = -1;
        int size = _intvars.size();

        IntExp[] vars = _intvars.data();

        for (int i = 0; i < size; ++i) {
            IntExp vari = vars[i];

            int maxi = vari.size();

            if (maxi != 1) {
                if ((max < maxi) || max_var == -1) {
                    max = maxi;
                    max_var = i;
                }
            }
        }
        return max_var;
    }

} // ~IntVarSelectorMaxSize
