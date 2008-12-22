package org.openl.rules.calc;

import java.util.HashMap;
import java.util.Map;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DynamicObject;
import org.openl.util.print.NicePrinter;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetResult implements IDynamicObject 
{
	
	boolean cacheResult = true;

	SpreadsheetType type;
	Spreadsheet calc;
	
	IDynamicObject targetModule; // OpenL module
	Object[] params;  // copy of the spreadsheet call params
	
	IRuntimeEnv env; //copy of the call environment
	
	
	Map<String, Object> results = new HashMap<String, Object>();
	
	
	
	public SpreadsheetResult(Spreadsheet calc, IDynamicObject targetModule,
			Object[] params, IRuntimeEnv env) {
		super();
		this.calc = calc;
		this.type = calc.getSpreadsheetType();
		this.targetModule = targetModule;
		this.params = params;
		this.env = env;
	}

	public Object getFieldValue(String name) 
	{
		if (cacheResult)
		{
			Object result = results.get(name);
			if (result != null)
				return result;
			result = targetModule.getFieldValue(name);
			if (result != null)
				return result;
		}	

		
		IOpenField f = type.getField(name);
		
		if (f == null)
			throw new RuntimeException("Spreadsheet field " + name + " not found");
		
		ASpreadsheetField sfield = (ASpreadsheetField)f;
		
		return sfield.calculate(this, targetModule, params, env);
		
		
	}

	public IOpenClass getType() {
		return type;
	}

	public void setFieldValue(String name, Object value) {
		// TODO Auto-generated method stub
		
	}

	public Object getColumn(int column, IRuntimeEnv env2) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getRow(int row, IRuntimeEnv env2) {
		// TODO Auto-generated method stub
		return null;
	}

	  public String toString()
	  {
	    NicePrinter printer = new NicePrinter();
	    printer.print(this, DynamicObject.getNicePrinterAdaptor());
	    return printer.getBuffer().toString();
	  }

	public Map<String, Object> getFieldValues() {
		return results;
	}

	
}
