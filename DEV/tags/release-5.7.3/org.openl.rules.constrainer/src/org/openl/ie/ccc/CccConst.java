package org.openl.ie.ccc;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000, 2002
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
 * Global constants for the CCC package
 */

public interface CccConst {
    static final int STATUS_UNKNOWN = 0;
    static final int STATUS_GREEN = 1;
    static final int STATUS_YELLOW = 2;
    static final int STATUS_INCOMPATIBLE = 3;
    static final int STATUS_INVALID = 4;
    static final int STATUS_INACTIVE = 5;
    static final int STATUS_ACTIVE = 6;

    static final int TYPE_UNKNOWN = 0;
    static final int TM_INT = 1 << 0;
    static final int TM_FLOAT = 1 << 1;
    static final int TM_SET = 1 << 2;
    static final int TM_JOB = 1 << 3;
    static final int TM_RESOURCE = 1 << 4;
    static final int TM_GOAL = 1 << 5;
    static final int TM_CONSTRAINT = 1 << 6;
    static final int TM_VARIABLE = 1 << 7;
    static final int TM_OBJECTIVE = 1 << 8;
    static final int TM_SOLUTION = 1 << 9;

    static final String GROUP_INT = "Integers";
    static final String GROUP_FLOAT = "Floats";
    static final String GROUP_SET = "Sets";
    static final String GROUP_JOB = "Jobs";
    static final String GROUP_RESOURCE = "Resources";

}
