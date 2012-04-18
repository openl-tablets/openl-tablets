package org.openl.rules.ce;

import java.util.ArrayList;
import java.util.List;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.IntVar;

public class SchedulerTest3 {
	
	
	static final int MAX_CPU = 4;
	static final int MAX_GROUPS = MAX_CPU +1;
	

	public static void main(String[] args) {
		
		
		
		Constrainer C = new Constrainer("scheduling");

		List<MyJob> jobs = new ArrayList<SchedulerTest3.MyJob>();
		List<Group> groups = new ArrayList<SchedulerTest3.Group>();
		
 		
		for (int i = 0; i < args.length; i++) {
	
			
			
		}
		
		
		
	}
	
	
	class MyJob
	{
		
		MyJob(Constrainer C, String name, int max_g_length)
		{
			this.name = name;
			group = C.addIntVar(0, MAX_GROUPS);
			index = C.addIntVar(0, max_g_length);
		}
		
		String name;
		
		IntVar group;
		IntVar index; 
		
	}
	
	
	class Group
	{
		int slot;
		int cpu;
	}
	
	
	
	
	

}
