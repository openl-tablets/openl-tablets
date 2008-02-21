/*
 * Created on Nov 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.impl;

import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

/**
 * @author snshor
 *
 */
public class ParameterDeclaration implements IParameterDeclaration
{
	IOpenClass type;
	String name;
	int direction;
	
	public ParameterDeclaration(IOpenClass type, String name)
	{
		this(type, name, IN);
	}
		


	public ParameterDeclaration(IOpenClass type, String name, int direction)
	{
		this.type = type;
		this.name = name;
		this.direction = direction;
	}
		
	
	/**
	 * @return
	 */
	public int getDirection()
	{
		return direction;
	}

	/**
	 * @return
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return
	 */
	public IOpenClass getType()
	{
		return type;
	}



	public String getDisplayName(int mode)
	{
		return name;
	}

}
