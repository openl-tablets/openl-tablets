/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl.module;

import org.openl.binding.AmbiguousVarException;
import org.openl.binding.DuplicatedVarException;
import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.LocalFrameBuilder;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 *
 */
public class MethodBindingContext extends BindingContextDelegator
{

	static final int STATUS_ADDING_PARAMS = 0, STATUS_ADDING_LOCAL_VARS = 1;

	LocalFrameBuilder localFrame = new LocalFrameBuilder();

	int paramFrameSize = 0;
	
	int status = STATUS_ADDING_PARAMS;
	
	IOpenClass returnType;
	IOpenMethodHeader header;

  /**
   * @param delegate
   */
  public MethodBindingContext(IOpenMethodHeader header,  IBindingContext delegate)
  {
    super(delegate);
    this.header = header;
    pushLocalVarContext();
    IMethodSignature signature = header.getSignature();
    IOpenClass[] params = signature.getParameterTypes();
    for (int i = 0; i < params.length; i++)
    {
      try
      {
        addParameter(ISyntaxConstants.THIS_NAMESPACE, signature.getParameterName(i), params[i]);
      }
      catch (DuplicatedVarException e)
      {
      	throw RuntimeExceptionWrapper.wrap("", e);
      }
    }
    
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBindingContext#addVar(java.lang.String, java.lang.String, org.openl.types.IOpenClass)
   */
  public ILocalVar addVar(String namespace, String name, IOpenClass type)
    throws DuplicatedVarException
  {
  	status = STATUS_ADDING_LOCAL_VARS;
    return localFrame.addVar(namespace, name, type);
  }

	public ILocalVar addParameter(String namespace, String name, IOpenClass type)
		throws DuplicatedVarException
	{
		if (status != STATUS_ADDING_PARAMS)
		  throw new IllegalStateException();
		paramFrameSize++;
		
		return localFrame.addVar(namespace, name, type);
	}




  /* (non-Javadoc)
   * @see org.openl.binding.IBindingContext#findVar(java.lang.String, java.lang.String)
   */
  public IOpenField findVar(String namespace, String name)
    throws AmbiguousVarException
  {
  	IOpenField var = localFrame.findLocalVar(namespace, name);
  	
    return var != null ? var : delegate.findVar(namespace, name);
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBindingContext#getLocalVarFrameSize()
   */
  public int getLocalVarFrameSize()
  {
    return localFrame.getLocalVarFrameSize();
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBindingContext#getParamFrameSize()
   */
  public int getParamFrameSize()
  {
    return paramFrameSize;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBindingContext#popLocalVarcontext()
   */
  public void popLocalVarContext()
  {
    localFrame.popLocalVarcontext();
  }

  public void pushLocalVarContext()
  {
    localFrame.pushLocalVarContext();
  }
  
  

  public IOpenClass getReturnType()
  {
    return returnType == null ?  header.getType() : returnType;
  }

  public void setReturnType(IOpenClass type)
  {
  	if (getReturnType() != NullOpenClass.the)
  		throw new RuntimeException("Can not override return type " + getReturnType());
  	returnType = type;
  }

}
