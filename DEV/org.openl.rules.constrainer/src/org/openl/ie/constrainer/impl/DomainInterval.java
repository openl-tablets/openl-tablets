package org.openl.ie.constrainer.impl;

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
 * Class DomainInterval implements integer intervals used by DomainImplWithHoles.
 *
 * @see DomainImplWithHoles
 */
public final class DomainInterval {
    public int from;
    public int to;

    public DomainInterval(int min, int max) {
        from = min;
        to = max;
    }

    @Override
    public String toString() {
        if (to == from) {
            return "" + to;
        } else {
            return "(" + from + ";" + to + ")";
        }
    }
}
