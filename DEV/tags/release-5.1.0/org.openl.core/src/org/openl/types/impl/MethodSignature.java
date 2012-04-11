/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 */
public class MethodSignature implements IMethodSignature
{

	IParameterDeclaration[] parameters;



	public MethodSignature(IOpenClass[] parTypes)
	{
		int len = parTypes.length;
		parameters = new IParameterDeclaration[len];
		for (int i = 0; i < len; i++)
		{
			parameters[i] = new ParameterDeclaration(parTypes[i], "p" + i);
		}
	}
	
	/**
	 * Copy constructor makes deep copy of ims
	 * @param ims
	 */
	public MethodSignature(IMethodSignature ims)
	{
		IOpenClass[] parTypes = ims.getParameterTypes();
		int len = parTypes.length;
		parameters = new IParameterDeclaration[len];
		for (int i = 0; i < len; i++)
		{
			parameters[i] = new ParameterDeclaration(parTypes[i], ims.getParameterName(i));
		}
	}
	

	public MethodSignature(IOpenClass[] parTypes, String[] names)
	{
		int len = parTypes.length;
		parameters = new IParameterDeclaration[len];
		for (int i = 0; i < len; i++)
		{
			parameters[i] = new ParameterDeclaration(parTypes[i], names[i]);
		}
	}

	/**
	 * 
	 */
	public MethodSignature(IParameterDeclaration[] parameters)
	{
		this.parameters = parameters;
	}

	public MethodSignature merge(IParameterDeclaration[] extraParams)
	{
		return new MethodSignature(
			(IParameterDeclaration[]) ArrayTool.merge(parameters, extraParams));
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IMethodSignature#getParameterTypes()
	 */
	public IOpenClass[] getParameterTypes()
	{
		IOpenClass[] parameterTypes = new IOpenClass[parameters.length];

		for (int i = 0; i < parameterTypes.length; i++)
		{
			parameterTypes[i] = parameters[i].getType();
		}
		return parameterTypes;
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IMethodSignature#getParameterName(int)
	 */
	public String getParameterName(int i)
	{
		return parameters[i].getName();
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IMethodSignature#getParameterDirection(int)
	 */
	public int getParameterDirection(int i)
	{
		return parameters[i].getDirection();
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IMethodSignature#getNumberOfParameters()
	 */
	public int getNumberOfArguments()
	{
		return parameters.length;
	}

}
