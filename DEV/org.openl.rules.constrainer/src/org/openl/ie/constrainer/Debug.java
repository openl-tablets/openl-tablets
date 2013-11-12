package org.openl.ie.constrainer;

import org.openl.util.Log;

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
 * Class Debug controls debug printing. Debug.print(string) prints the string
 * only if debug printing is on.
 */
public class Debug {
    static private boolean yes = false;

    /**
     * Turns printing off.
     */
    static public void off() {
        yes = false;
    }

    /**
     * Turns printing on.
     */
    static public void on() {
        yes = true;
    }

    /**
     * Prints the string if printing is on.
     */
    static public void print(String s) {
        if (Debug.yes) {
            Log.debug(s);
        }
    }

} // ~Debug
