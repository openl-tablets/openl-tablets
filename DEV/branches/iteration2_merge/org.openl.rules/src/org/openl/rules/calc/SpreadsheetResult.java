package org.openl.rules.calc;

import java.util.HashMap;
import java.util.Map;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DynamicObject;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetResult implements IDynamicObject 
{
	
	boolean cacheResult = true;

	SpreadsheetType type;
	Spreadsheet calc;
	
	DynamicObject targetModule; // OpenL module
	Object[] params;  // copy of the spreadsheet call params
	
	IRuntimeEnv env; //copy of the call environment
	
	
	Map<String, Object> results = new HashMap<String, Object>();
	
	
	
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

	
	
}
