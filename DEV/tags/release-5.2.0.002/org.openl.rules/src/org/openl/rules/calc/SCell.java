package org.openl.rules.calc;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class SCell 
{
	
	int row, column;
	


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

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	public SCell(int row, int column) {
		super();
		this.row = row;
		this.column = column;
	}
	
	
	public Object calculate(SpreadsheetResult spreadsheetResult,
			Object targetModule, Object[] params, IRuntimeEnv env) 
	{
		if (isValueCell())
		{	
			Object value = getValue();
			if (value instanceof AnyCellValue)
				return ((AnyCellValue)value).getValue();
			return value;
		}	
		
		else if (isMethodCell())
			return getMethod().invoke(spreadsheetResult, params, env);
		
		else 
			return null;
	}
	

}
