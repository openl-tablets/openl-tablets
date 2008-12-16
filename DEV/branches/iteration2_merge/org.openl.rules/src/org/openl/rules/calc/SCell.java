package org.openl.rules.calc;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class SCell 
{

	public enum CellKind{VALUE, METHOD, EMPTY};
	
	CellKind kind = CellKind.EMPTY;
	Object value;
	IOpenClass type;
	
	IOpenMethod method;
	
	public boolean isValueCell() {
		return kind == CellKind.VALUE;
	}

	public IOpenClass getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	public boolean isMethodCell() {
		return kind == CellKind.METHOD;
	}

	public IOpenMethod getMethod() {
		return method;
	}
	

}
