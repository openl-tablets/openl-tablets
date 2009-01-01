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
		return cell.calculate(spreadsheetResult, targetModule, params, env);
	}

	@Override
	public IOpenClass getType() {
		return cell.getType();
	}

	public SCell getCell() {
		return cell;
	}
	
}
