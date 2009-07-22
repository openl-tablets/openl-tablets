package org.openl.codegen;

import org.openl.types.IOpenClass;

public interface ICodeGenContext 
{

	/**
	 * 
	 * @param rt
	 * @return the best String representation for the type to use in the code.
	 */
	
	String addReferredType(IOpenClass rt);
	
	boolean addNewName(String name, String type);
	
	String genNewName(String baseName, String type);
	
	ICodeGenContext parentContext();
	
}
