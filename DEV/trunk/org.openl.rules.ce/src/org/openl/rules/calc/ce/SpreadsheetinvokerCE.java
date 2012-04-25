package org.openl.rules.calc.ce;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetInvoker;
import org.openl.types.IDynamicObject;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.IScheduledActivity;
import org.openl.util.ce.IScheduler;
import org.openl.util.ce.impl.ScheduleExecutor;
import org.openl.util.ce.impl.ServiceMT;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetinvokerCE extends SpreadsheetInvoker {

//TODO this is a vry first approximation; we need to use spreadsheet properties and/or measurements and code analysis techniques to calculate this time dynamically 	
	static final long DEFAULT_CELL_TIME_NS = 10000;
	
	@Override
	public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
		SpreadsheetResultCalculatorCE calc = new SpreadsheetResultCalculatorCE(
				getInvokableMethod(), (IDynamicObject) target, params, env,
//				(Object[][])null);
				preFetchedResult, scheduleExecutor);
		
		calc.calculateAll();
		return getInvokableMethod().getResultBuilder().makeResult(calc);
	}

	IScheduledActivity[] scheduledActivities;
	ScheduleExecutor scheduleExecutor;
	
	public SpreadsheetinvokerCE(Spreadsheet spreadsheet) {
		super(spreadsheet);
		IActivity[] cellActivities =  new CellActivityBuilder(spreadsheet.getCells()).buildActivities();
		IScheduler scheduler = ServiceMT.getService().getScheduler(DEFAULT_CELL_TIME_NS);
		
		this.scheduledActivities =  scheduler.prepare(cellActivities);
		this.scheduleExecutor = new ScheduleExecutor(scheduledActivities);

	}


}
