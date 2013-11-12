package org.openl.rules.ce;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.scheduler.AlternativeResourceSet;
import org.openl.ie.scheduler.GoalSetTimes;
import org.openl.ie.scheduler.Job;
import org.openl.ie.scheduler.Resource;
import org.openl.ie.scheduler.Schedule;

public class SchedulerTest2 {
	
	
	
	
	static public void main(String[] pars) throws Failure
	{
		
		Constrainer C = new Constrainer("scheduling");
		Schedule s = new Schedule(C, 0, 100) ;
		
		
		Resource[] cpu = new Resource[4]; 
		
		AlternativeResourceSet cpuset = new AlternativeResourceSet();
		for (int i = 0; i < cpu.length; i++) {
			cpu[i] = s.addResourceDiscrete(1, "CPU-"+i);
			cpuset.add(cpu[i]);
		}
		
		
		int N = 16;
		Job[] jj = new Job[N];
		for (int i = 0; i < jj.length; i++) {
			jj[i] = s.addJob(1, "J-"+i);
			C.postConstraint(jj[i].requires(cpuset, 1));
		}
		
		
		C.postConstraint(jj[0].startsAfterEnd(jj[1]));
		
        Goal solution = new GoalSetTimes(s.jobs(), null);

        long solveStart = System.currentTimeMillis();

        // C.traceExecution();
        // C.traceFailures();
        // C.trace(vars);

        C.printInformation();
        Goal gm = solution;
        
        C.execute(gm);
        
        
        for (int i = 0; i < jj.length; i++) {
			System.out.println(jj[i]);
		}
        
        System.out.println(s.resources());
		
		
		
	}
	
	
	

}
