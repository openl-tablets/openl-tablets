package scheduler;

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

import org.openl.ie.constrainer.*;
import org.openl.ie.scheduler.*;

/**
 */
public final class Oven
{

    static class MySelector implements JobVariableSelector {
      public IntVarSelector getSelector(IntExpArray vars)
      {
        return new IntVarSelectorMinSizeMin(vars);
      }
    }

    public static void main(String args[]) throws Exception
    {
        Constrainer C = new Constrainer("Oven Scheduling Example");
        Schedule S = new Schedule(C,0,11);
        S.setName("Oven");

        long executionStart = System.currentTimeMillis();

        Job jA = S.addJob(1, "JobA");
        Job jB = S.addJob(4, "JobB");
        Job jC = S.addJob(4, "JobC");
        Job jD = S.addJob(2, "JobD");
        Job jE = S.addJob(4, "JobE");

        Resource oven = S.addResourceDiscrete(3,"oven");

        oven.setCapacityMax(0,2);
        oven.setCapacityMax(1,1);
        oven.setCapacityMax(2,0);
        oven.setCapacityMax(3,1);
        oven.setCapacityMax(4,1);
        oven.setCapacityMax(10,1);

        jA.requires(oven,2).post();
        jB.requires(oven,1).post();
        jC.requires(oven,1).post();
        jD.requires(oven,1).post();
        jE.requires(oven,2).post();

        Goal solution = new GoalSetTimes(S.jobs(), new MySelector());

        long solveStart = System.currentTimeMillis();

        C.printInformation();

        if (!C.execute(solution))
            System.out.println("Can not solve "+solution);
        else
        {
            System.out.println("1st solution:");
            for(int i=0; i < S.jobs().size(); ++i)
            {
                Job job = (Job)S.jobs().elementAt(i);
                System.out.println(job);
            }
        }
        System.out.println(oven);

        long solveTime = System.currentTimeMillis() - solveStart;
        long executionTime = System.currentTimeMillis() - executionStart;

        System.out.println("Execution time: "+executionTime+" msec");
        System.out.println("Solving time: "+solveTime+" msec");

    }
}
