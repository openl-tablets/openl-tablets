package org.openl.ie.constrainer;

import java.io.Serializable;

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
//
//: Expression.java
//

/**
 * An interface for the constrained integer and floating-point expressions.
 */

public interface Expression extends Subject, EventOfInterest.Constants, Serializable {

    /**
     * Checks wether the expression is linear or not.
     *
     * @return true if the expression is linear, false otherwise
     */
    boolean isLinear();

} // ~Expression
