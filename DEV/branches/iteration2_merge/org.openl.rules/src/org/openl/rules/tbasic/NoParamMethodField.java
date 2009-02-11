/**
 * 
 */
package org.openl.rules.tbasic;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class NoParamMethodField implements IOpenField
{
	
	String labelName; 
	AlgorithmSubroutineMethod smethod;

	public NoParamMethodField(String labelName, AlgorithmSubroutineMethod smethod) 
	{
		this.labelName = labelName;
		this.smethod = smethod;
	}

	public Object get(Object target, IRuntimeEnv env) {
		return smethod.invoke(target, new Object[]{}, env);
	}

	public boolean isConst() {
		return false;
	}

	public boolean isReadable() {
		return true;
	}

	public boolean isWritable() {
		return false;
	}

	public void set(Object target, Object value, IRuntimeEnv env) {
		
	}

	public IOpenClass getDeclaringClass() {
		return smethod.getDeclaringClass();
	}

	public IMemberMetaInfo getInfo() {
		return null;
	}

	public IOpenClass getType() {
		return smethod.getType();
	}

	public boolean isStatic() {
		return false;
	}

	public String getDisplayName(int mode) {
		return smethod.getDisplayName(mode);
	}

	public String getName() {
		return labelName;
	}
	
}