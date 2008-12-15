package org.openl.rules.calc;

import org.openl.types.IOpenSchema;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicObject;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetType extends ADynamicClass 
{

	public SpreadsheetType(IOpenSchema schema, String name) {
		super(schema, name, DynamicObject.class);
	}

	public Object newInstance(IRuntimeEnv env) {
		return new DynamicObject(this);
	}

}
