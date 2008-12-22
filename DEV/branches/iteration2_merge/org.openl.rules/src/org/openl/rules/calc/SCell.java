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

	public CellKind getKind() {
		return kind;
	}

	public void setKind(CellKind kind) {
		this.kind = kind;
	}

	public void setValue(Object value) 
	{
		if (value == null)
			this.kind = CellKind. EMPTY;
		else if (value instanceof IOpenMethod)
		{
			this.kind = CellKind. METHOD;
			this.method = (IOpenMethod)value;
		}	
		else
		{	
			this.value = value;
			this.kind = CellKind.VALUE;
		}	
	}

	public void setType(IOpenClass type) {
		this.type = type;
	}

	public void setMethod(IOpenMethod method) {
		this.method = method;
	}
	

}
