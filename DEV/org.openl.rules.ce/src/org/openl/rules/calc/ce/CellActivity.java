package org.openl.rules.calc.ce;

import java.util.List;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.IInvokableActivity;
import org.openl.vm.IRuntimeEnv;

public class CellActivity implements IInvokableActivity{

	
	@Override
	public String toString() {
		return cell.getMethod().toString(); // + dependsOn;
	}

	SpreadsheetCell cell;
	public SpreadsheetCell getCell() {
		return cell;
	}

	List<IActivity> dependsOn;
	
	public CellActivity(SpreadsheetCell cell) {
		super();
		this.cell = cell;
	}

	@Override
	public List<IActivity> dependsOn() {
		return dependsOn;
	}

	@Override
	public long duration() {
		return 1000;
	}

	@Override
	public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
		 Object res =  cell.getMethod().invoke(target, params, env);
		 
		 SpreadsheetResultCalculatorCE calc = (SpreadsheetResultCalculatorCE)target;
		 
		 calc.setValue(cell.getRowIndex(), cell.getColumnIndex(), res);
		 
		 return res;
	}

	public void setDependsOn(List<IActivity> dependsOn) {
		this.dependsOn = dependsOn;
	}

	
	
	
}
