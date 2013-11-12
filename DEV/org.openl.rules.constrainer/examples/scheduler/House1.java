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
public final class House1
{
    public static void main(String args[]) throws Exception
    {
        Constrainer C = new Constrainer("House 1 Example");
        Schedule S = new Schedule(C, 0, 30);
        S.setName("House 1");

        // defining jobs
        Job masonry = S.addJob(7, "masonry");
        Job carpentry = S.addJob(3, "carpentry");
        Job roofing = S.addJob(1, "roofing");
        Job plumbing = S.addJob(8, "plumbing");
        Job ceiling = S.addJob(3, "ceiling");
        Job windows = S.addJob(1, "windows");
        Job facade = S.addJob(2, "facade");
        Job garden = S.addJob(1, "garden");
        Job painting = S.addJob(2, "painting");
        Job moving_in = S.addJob(1, "moving_in");

        //    Posting "startsAfterEnd" constraints
        carpentry.startsAfterEnd(masonry).asConstraint().post();
        roofing.startsAfterEnd(carpentry).asConstraint().post();
        plumbing.startsAfterEnd(masonry).asConstraint().post();
        ceiling.startsAfterEnd(masonry).asConstraint().post();
        windows.startsAfterEnd(roofing).asConstraint().post();
        facade.startsAfterEnd(roofing).asConstraint().post();
        facade.startsAfterEnd(plumbing).asConstraint().post();
        garden.startsAfterEnd(roofing).asConstraint().post();
        garden.startsAfterEnd(plumbing).asConstraint().post();
        painting.startsAfterEnd(ceiling).asConstraint().post();
        moving_in.startsAfterEnd(windows).asConstraint().post();
        moving_in.startsAfterEnd(facade).asConstraint().post();
        moving_in.startsAfterEnd(garden).asConstraint().post();
        moving_in.startsAfterEnd(painting).asConstraint().post();

        C.printInformation();
        Goal solution = new GoalSetTimes(S.jobs());

        IntExp objective = moving_in.getStartVariable();
        if (!C.execute(new GoalMinimize(solution,objective)))
            System.out.println("Can not minimize cost "+objective);
        else
        {
            System.out.println("Optimal solution with objective="+objective+":");
            for(int i=0; i < S.jobs().size(); ++i)
            {
                Job job = (Job)S.jobs().elementAt(i);
                System.out.println(job);
            }
        }
    }
}
