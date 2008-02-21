/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.List;

import org.openl.OpenL;
import org.openl.binding.AmbiguousMethodException;
import org.openl.binding.AmbiguousVarException;
import org.openl.binding.DuplicatedVarException;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.ILocalVar;
import org.openl.binding.INodeBinder;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public class BindingContextDelegator implements IBindingContextDelegator
{

	protected IBindingContext delegate;

	public BindingContextDelegator(IBindingContext delegate)
	{
		this.delegate = delegate;
	}

	/**
	 * @param error
	 */
	public void addError(ISyntaxError error)
	{
		delegate.addError(error);
	}

	/**
	 * @param namespace
	 * @param name
	 * @param type
	 * @return
	 * @throws DuplicatedVarException
	 */
	public ILocalVar addVar(String namespace, String name, IOpenClass type)
		throws DuplicatedVarException
	{
		return delegate.addVar(namespace, name, type);
	}

	/**
	 * @param node
	 * @return
	 */
	public INodeBinder findBinder(ISyntaxNode node)
	{
		return delegate.findBinder(node);
	}

	/**
	 * @param namespace
	 * @param name
	 * @param parTypes
	 * @return
	 * @throws AmbiguousMethodException
	 */
	public IMethodCaller findMethodCaller(
		String namespace,
		String name,
		IOpenClass[] parTypes)
		throws AmbiguousMethodException
	{
		return delegate.findMethodCaller(namespace, name, parTypes);
	}

	/**
	 * @param namespace
	 * @param typeName
	 * @return
	 */
	public IOpenClass findType(String namespace, String typeName)
	{
		return delegate.findType(namespace, typeName);
	}

	/**
	 * @param namespace
	 * @param name
	 * @return
	 * @throws AmbiguousVarException
	 */
	public IOpenField findVar(String namespace, String name, boolean strictMatch)
		throws AmbiguousVarException
	{
		return delegate.findVar(namespace, name, strictMatch);
	}

	/**
	 * @param from
	 * @param to
	 * @return
	 */
	public IOpenCast getCast(IOpenClass from, IOpenClass to)
	{
		return delegate.getCast(from, to);
	}

	/**
	 * @return
	 */
	public ISyntaxError[] getError()
	{
		return delegate.getError();
	}

	/**
	 * @return
	 */
	public int getLocalVarFrameSize()
	{
		return delegate.getLocalVarFrameSize();
	}

	/**
	 * @return
	 */
	public int getNumberOfErrors()
	{
		return delegate.getNumberOfErrors();
	}

	/**
	 * @return
	 */
	public int getParamFrameSize()
	{
		return delegate.getParamFrameSize();
	}

	/**
	 * 
	 */
	public void popLocalVarContext()
	{
		delegate.popLocalVarContext();
	}

	/**
	 * 
	 */
	public void pushLocalVarContext()
	{
		delegate.pushLocalVarContext();
	}

	/**
	 * @param namespace
	 * @param name
	 * @param type
	 * @return
	 * @throws DuplicatedVarException
	 */
	public ILocalVar addParameter(
		String namespace,
		String name,
		IOpenClass type)
		throws DuplicatedVarException
	{
		return delegate.addParameter(namespace, name, type);
	}

	/**
	 * @return
	 */
	public IOpenClass getReturnType()
	{
		return delegate.getReturnType();
	}

	/**
	 * @param name
	 * @param value
	 */
	public void addAlias(String name, String value)
	{
		delegate.addAlias(name, value);
	}

	/**
	 * @param name
	 * @return
	 */
	public String getAlias(String name)
	{
		return delegate.getAlias(name);
	}

	/* (non-Javadoc)
	 * @see org.openl.binding.IBindingContextDelegator#setTopDelegate(org.openl.binding.IBindingContext)
	 */
	public void setTopDelegate(IBindingContext delegate)
	{
		if (this.delegate == null)
		{
			this.delegate = delegate;
			return;
		}

		if (this.delegate instanceof IBindingContextDelegator)
			((IBindingContextDelegator) this.delegate).setTopDelegate(delegate);
	}

	public void addType(String namespace, IOpenClass type) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	

  /**
   * @param errors
   */
//  public void addAllErrors(Vector errors)
//  {
//    delegate.addAllErrors(errors);
//  }
  /**
   * @return
   */
  public List<ISyntaxError> popErrors()
  {
    return delegate.popErrors();
  }
  /**
   * 
   */
  public void pushErrors()
  {
    delegate.pushErrors();
  }

	public void setReturnType(IOpenClass type)
	{
		this.delegate.setReturnType(type);
	}

	public IOpenField findFieldFor(IOpenClass type, String fieldName,
		boolean strictMatch)
	{
	    return delegate.findFieldFor(type, fieldName, strictMatch);
	}

	public OpenL getOpenL()
	{
	    return delegate.getOpenL();
	}
}
