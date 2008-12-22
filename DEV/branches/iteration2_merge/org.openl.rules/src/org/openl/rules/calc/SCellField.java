package org.openl.rules.calc;

import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SCellField extends ASpreadsheetField {

	public SCellField(IOpenClass declaringClass, String name, SCell cell) {
		super(declaringClass, name, cell.getType());
		this.cell = cell;
	}

	SCell cell;

	@Override
	public Object calculate(SpreadsheetResult spreadsheetResult,
			Object targetModule, Object[] params, IRuntimeEnv env) 
	{
		if (cell.isValueCell())
			return cell.getValue();
		
		if (cell.isMethodCell())
			return cell.getMethod().invoke(spreadsheetResult, params, env);
		
		return null;
	}
	
}
