package org.openl.rules.calc.ce;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.types.IDynamicObject;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.IInvokableActivity;
import org.openl.util.ce.InvokeFactory;
import org.openl.util.ce.impl.ScheduleExecutor;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetResultCalculatorCE extends SpreadsheetResultCalculator {

	
	
	ScheduleExecutor scheduleExecutor;
	
	public SpreadsheetResultCalculatorCE(Spreadsheet spreadsheet,
			IDynamicObject targetModule, Object[] params, IRuntimeEnv env,
			Object[][] preCalculatedResult, ScheduleExecutor schex) {
		
		super(spreadsheet, targetModule, params, env, preCalculatedResult);
		
		this.scheduleExecutor =  schex; 
		
		
	}

	public void calculateAll() {
		InvokeFactory factory = new InvokeFactory(){

			@Override
			public Object invoke(IActivity activity) {
				IInvokableActivity invokableActivity = (IInvokableActivity)activity;
				
				return invokableActivity.invoke(SpreadsheetResultCalculatorCE.this, params, env.cloneEnvForMT());
			}};
		
		scheduleExecutor.execute(factory);
	}


}
