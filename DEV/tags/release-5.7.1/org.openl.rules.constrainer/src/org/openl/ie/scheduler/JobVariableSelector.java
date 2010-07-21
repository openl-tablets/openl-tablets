package org.openl.ie.scheduler;

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
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVarSelector;

/**
 * Interface JobVariableSelector helps using user-defined selectors while
 * instantiating variables inside GoalSetTimes
 *
 * <p>
 * Example:
 *
 * <pre>
 *     static class MySelector implements JobVariableSelector {
 *      public IntVarSelector getSelector(IntExpArray vars)
 *      {
 *        return new IntVarSelectorFirstUnbound(vars);
 *      }
 *    }
 * ...
 * Goal solution = new GoalSetTimes(schedule.jobs(), new MySelector());
 *
 * &#064;see GoalSetTimes
 *
 */
public interface JobVariableSelector {

    public IntVarSelector getSelector(IntExpArray vars);
}