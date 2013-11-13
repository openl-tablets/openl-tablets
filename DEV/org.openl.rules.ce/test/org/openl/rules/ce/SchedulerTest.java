package org.openl.rules.ce;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.ICallableActivity;
import org.openl.util.ce.IScheduledActivity;
import org.openl.util.ce.conf.ServiceMTConfiguration;
import org.openl.util.ce.impl.InvokeCallableFactory;
import org.openl.util.ce.impl.ScheduleExecutor;
import org.openl.util.ce.impl.ServiceMT;
import org.openl.util.ce.impl.ServiceMT16;

public class SchedulerTest {

	
	IActivity[] activities;
	
	@Test
	public void test() {
		prepareData();
		
		ServiceMT.setService(new ServiceMT16(new ServiceMTConfiguration())); 
//		ServiceMT.setService(new ServiceMT17(new ServiceMTConfiguration())); 
//		ServiceMT.setService(new ServiceST(new ServiceMTConfiguration())); 
		
		IScheduledActivity[] saa = ServiceMT.getService().getScheduler(100000).prepare(activities);
		
		ScheduleExecutor se = new ScheduleExecutor(saa);
		
		se.execute(new InvokeCallableFactory());
		
	}

	private void prepareData() {
		
		
		TestAct LD = new TestAct( "ld!\n");
		TestAct HEL = new TestAct( "Hel");
		TestAct WO = new TestAct( "wo");
		TestAct LO = new TestAct( "lo");
		TestAct COMMA = new TestAct( ", ");
		TestAct R = new TestAct( "r");
		
		
		dep(HEL, LO);
		dep(LO, COMMA);
		dep(COMMA, WO);
		dep(WO, R);
		dep(R, LD);
		
		activities = new IActivity[]{LD, HEL,WO , LO, COMMA, R };
	}

	
	private void dep(TestAct prec, TestAct dep) {
		dep.dependsOn.add(prec); 
	}


	class TestAct implements ICallableActivity<String>
	{
		
		@Override
		public String toString() {
			return src;
		}

		List<IActivity> dependsOn = new ArrayList<IActivity>();
		
		String src;

		public TestAct(String string) {
			this.src = string;
		}

		@Override
		public long duration() {
			return 1;
		}

		@Override
		public List<IActivity> dependsOn() {
			return dependsOn;
		}

		@Override
		public String call() throws Exception {
			String res = src.toUpperCase();
			System.out.print(res);
			return res;
		}
		
	}
}
